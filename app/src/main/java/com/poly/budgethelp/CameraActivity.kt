package com.poly.budgethelp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.poly.budgethelp.graphics.GraphicOverlay
import com.poly.budgethelp.graphics.GraphicOverlay.Graphic
import com.poly.budgethelp.graphics.TextGraphic
import com.poly.budgethelp.utility.ActivityUtils
import com.poly.budgethelp.utility.TextUtils
import com.poly.budgethelp.viewmodel.WordToIgnoreViewModel
import com.poly.budgethelp.viewmodel.WordToIgnoreViewModelFactory
import kotlinx.coroutines.launch
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

    private var addToExistingReceipt = false

    private var validLines: ArrayList<Pair<String, Rect>> = ArrayList()

    private var imageViewWidth: Int? = null
    private var imageViewHeight: Int? = null

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
        captureButton.setOnClickListener { _ -> takePicture() }

        wordsToIgnore = arrayListOf()
        // Set words to ignore
        wordToIgnoreViewModel.allWords.observe(this) { words ->
            words.let {
                it.forEach {toIgnore -> wordsToIgnore.add(toIgnore.word)}
            }
        }

        // Is this activity continuing an existing receipt
        val existingReceipt: Boolean? = intent.extras?.getBoolean(NewReceiptActivity.EXTRA_LOAD_PRODUCTS)
        if (existingReceipt != null)
            addToExistingReceipt = true
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
                            processText(visionText, inputImage)
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
            Toast.makeText(baseContext, resources.getString(R.string.error_camera_permissions_not_granted), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            startCamera()
        }
    }

    private fun processText(visionText: Text, inputImage: InputImage) {
        val items: ArrayList<Pair<String, Rect>> = ArrayList()
        val prices: ArrayList<Pair<Float, Rect>> = ArrayList()
        val itemWithPrice: ArrayList<Pair<String, Float>> = ArrayList()

        validLines.clear()

        lifecycleScope.launch {
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    if (wordsToIgnore.contains(line.text.uppercase()))
                        continue

                    // Force possible floats to use decimal point instead of a comma
                    val text = line.text.replace(",", ".")

                    val textValue: Float? = text.replace(" ", "").toFloatOrNull()
                    if (textValue != null && textValue <= UserConfig.productMaxPrice) {
                        val bb = line.boundingBox
                        if (bb != null) {
                            val element = Pair(text.replace(" ", "").toFloat(), bb)
                            prices.add(element)
                        }
                    }

                    else {
                        val bb = line.boundingBox
                        if (bb != null) {
                            // Seems very common to misinterpret i with |, so do this replace here
                            val receiptText = line.text.replace("|", "I")
                            val element = Pair(TextUtils.sanitizeText(receiptText), bb)
                            items.add(element)
                        }
                    }
                }
            }

            // Connect items and prices based on coordinates
            // FUTURE IMPROVEMENTS TO TEST:
            // - Sort prices based on offset, select one with least offset
            val maxOffset = UserConfig.priceNameMaxOffset
            for (pair in items) {
                for (price in prices) {
                    if (abs(pair.second.centerY() - price.second.centerY()) < maxOffset) {
                        itemWithPrice.add(Pair(pair.first, price.first))
                        validLines.add(Pair(pair.first, pair.second))
                        validLines.add(Pair(price.first.toString(), price.second))
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

        for (pair in itemWithPrice) {
            productCount++
            Log.d(TAG, pair.first + " : " + pair.second)
            builder.append(pair.first.uppercase()).append(NewReceiptActivity.saveFileDelimiter).append(pair.second).append(System.lineSeparator())
        }

        createImagePopup(builder, inputImage, productCount)
    }

    private fun createImagePopup(builder: StringBuilder, inputImage: InputImage, count: Int) {
        val popupData = ActivityUtils.createPopup(R.layout.popup_image_preview, this)

        val imageView: ImageView = popupData.first.findViewById(R.id.imageView)
        val graphicOverlay: GraphicOverlay = popupData.first.findViewById(R.id.graphicOverlay)
        val infoText: TextView = popupData.first.findViewById(R.id.previewImageText)
        val confirmButton: Button = popupData.first.findViewById(R.id.confirmSelectButton)
        val cancelButton: View = popupData.first.findViewById(R.id.cancelSelectButton)

        if (addToExistingReceipt)
            infoText.text = resources.getString(R.string.camera_number_of_items_existing, count)
        else
            infoText.text = resources.getString(R.string.camera_number_of_items, count)

        popupData.first.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                popupData.first.viewTreeObserver.removeOnGlobalLayoutListener(this)
                imageViewWidth = imageView.width
                imageViewHeight = imageView.height
                Log.d(TAG, "Image Width: $imageViewWidth, Image Height: $imageViewHeight")
                graphicOverlay.setCameraInformation(inputImage.width, inputImage.height)

                for (line in validLines) {
                    val textGraphic: Graphic = TextGraphic(graphicOverlay, android.util.Pair(line.first, line.second))
                    graphicOverlay.add(textGraphic)
                }
            }
        })

        popupData.second.setOnDismissListener {
            if (currentPopup == popupData.second)
                currentPopup = null
        }

        currentPopup?.dismiss()
        currentPopup = popupData.second
        val bmp: Bitmap? = inputImage.bitmapInternal
        if (bmp != null) {
            imageView.setImageBitmap(bmp)
        }

        graphicOverlay.clear()
        confirmButton.setOnClickListener { _ ->
            val intent = Intent(this, NewReceiptActivity::class.java)
            intent.putExtra(EXTRA_MESSAGE, builder.toString())
            intent.putExtra(NewReceiptActivity.EXTRA_LOAD_PRODUCTS, addToExistingReceipt)
            startActivity(intent)
            finish()
        }

        cancelButton.setOnClickListener { _ ->
            popupData.second.dismiss()
            captureButton.isClickable = true
        }
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