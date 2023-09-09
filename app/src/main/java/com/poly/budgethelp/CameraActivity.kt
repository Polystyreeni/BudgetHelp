package com.poly.budgethelp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.poly.budgethelp.config.UserConfig
import com.poly.budgethelp.databinding.ActivityCameraBinding
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.viewmodel.WordToIgnoreViewModel
import com.poly.budgethelp.viewmodel.WordToIgnoreViewModelFactory
import kotlinx.coroutines.launch
import java.lang.StringBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class CameraActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var captureButton: View

    private lateinit var textRecognizer: TextRecognizer

    private lateinit var wordsToIgnore: ArrayList<String>
    private var currentPopup: PopupWindow? = null

    // Viewmodels
    private val wordToIgnoreViewModel: WordToIgnoreViewModel by viewModels {
        WordToIgnoreViewModelFactory((application as BudgetApplication).wordToIgnoreRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "CameraActivity onCreate()")

        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            Log.d(TAG, "StartCamera called")
            startCamera()
        }
        else {
            Log.d(TAG, "Request permissions called")
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        captureButton = findViewById(R.id.fab)
        captureButton.setOnClickListener { view -> takePicture() }

        wordsToIgnore = arrayListOf()
        // Set words to ignore
        wordToIgnoreViewModel.allWords.observe(this) { words ->
            words.let {
                it.forEach {toIgnore -> wordsToIgnore.add(toIgnore.word)}
            }
        }
    }

    override fun onStop() {
        super.onStop()
        currentPopup?.dismiss()
        currentPopup = null
    }

    private fun takePicture() {
        // Stable reference to the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        createLoadPopup()
        captureButton.isClickable = false

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // super.onCaptureSuccess(image)
                Log.d(TAG, "Image capture success")
                val mediaImage = image.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                    textRecognizer.process(inputImage)
                        .addOnSuccessListener { visionText ->
                            processText(visionText)
                            image.close()
                        }
                        .addOnFailureListener { e ->
                            image.close()
                        }
                } else {
                    super.onCaptureSuccess(image)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                super.onError(exception)
                Log.d(TAG, "Image capture fail")
                currentPopup?.dismiss()
                currentPopup = null
                captureButton.isClickable = true
            }
        })
    }

    private fun startCamera() {
        Log.d(TAG, "Start camera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    override fun onDestroy() {
        super.onDestroy()

        cameraExecutor.shutdown()
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value)
                permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(baseContext, "Permission request denied", Toast.LENGTH_SHORT).show()
        } else {
            startCamera()
        }
    }

    private fun processText(visionText: Text) {
        val items: ArrayList<Pair<String, Int>> = ArrayList()
        val prices: ArrayList<Pair<Float, Int>> = ArrayList()
        val itemWithPrice: ArrayList<Pair<String, Float>> = ArrayList()

        lifecycleScope.launch {
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    if (wordsToIgnore.contains(line.text.uppercase()))
                        continue

                    val text = line.text.replace(",", ".").replace(":", " ")
                    val textValue: Float? = text.toFloatOrNull()
                    if (textValue != null && textValue <= UserConfig.productMaxPrice) {
                        val y = line.boundingBox?.centerY()
                        if (y != null) {
                            val element = Pair(text.toFloat(), y)
                            prices.add(element)
                        }
                    }

                    else {
                        val y = line.boundingBox?.centerY()
                        if (y != null) {
                            val element = Pair(line.text, y)
                            items.add(element)
                        }
                    }
                }
            }

            // Connect items and prices based on coordinates
            val maxOffset = UserConfig.priceNameMaxOffset
            for (pair in items) {
                for (price in prices) {
                    if (abs(pair.second - price.second) < maxOffset) {
                        itemWithPrice.add(Pair(pair.first, price.first))
                    }
                }
            }
        }

        if (itemWithPrice.size <= 0) {
            currentPopup?.dismiss()
            currentPopup = null
            Toast.makeText(baseContext, resources.getString(R.string.error_no_text_found), Toast.LENGTH_SHORT).show()
            captureButton.isClickable = true
            return
        }

        val builder = StringBuilder()
        var productCount = 0
        for(pair in itemWithPrice) {
            productCount++
            Log.d(TAG, pair.first + " : " + pair.second)
            builder.append(pair.first.uppercase()).append(":").append(pair.second).append(System.lineSeparator())
        }

        createAlert(productCount, builder)
    }

    private fun createAlert(productCount: Int, data: StringBuilder) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.camera_reading_complete))
        builder.setCancelable(false)

        builder.setMessage(resources.getString(R.string.camera_number_of_items, productCount))
        builder.setPositiveButton(resources.getString(R.string.camera_alert_positive)) {dialogInterface, _ ->
            // Start new activity
            val intent = Intent(this, NewReceiptActivity::class.java)
            intent.putExtra(EXTRA_MESSAGE, data.toString())
            startActivity(intent)
            dialogInterface.dismiss()
            finish()
        }

        builder.setNegativeButton(resources.getString(R.string.camera_alert_negative)) { dialogInterface, _ ->
            dialogInterface.dismiss()
            currentPopup?.dismiss()
            captureButton.isClickable = true
        }

        builder.show()
    }

    private fun createLoadPopup() {
        val popupData = ActivityUtils.createPopup(R.layout.popup_loading, this)
        val loadTextView: TextView = popupData.first.findViewById(R.id.loadPopupDescription)
        loadTextView.text = resources.getString(R.string.load_process_camera_image)

        currentPopup = popupData.second
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val EXTRA_MESSAGE = "CameraActivityProducts"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}