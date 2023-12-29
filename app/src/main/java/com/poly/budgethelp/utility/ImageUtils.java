package com.poly.budgethelp.utility;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import io.reactivex.rxjava3.annotations.Nullable;

@Nullable
public class ImageUtils {
    private static final String TAG = "ImageUtils";
    public static Bitmap getBitmap(InputImage inputImage) {
        if (inputImage == null) {
            Log.d(TAG, "InputImage is null");
            return null;
        }

        ByteBuffer data = inputImage.getByteBuffer();
        if (data == null) {
            Log.d(TAG, "ByteBufferData is null");
            return null;
        }

        data.rewind();
        byte[] imageInBuffer = new byte[data.limit()];
        data.get(imageInBuffer, 0, imageInBuffer.length);

        try {
            YuvImage image = new YuvImage(imageInBuffer, ImageFormat.NV21,
                    inputImage.getWidth(), inputImage.getHeight(), null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0,
                    inputImage.getWidth(), inputImage.getHeight()), 80, stream);
            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
            return rotateBitmap(bmp, inputImage.getRotationDegrees(), false, false);

        } catch (Exception e) {
            Log.d(TAG, "GetBitmap failed with: " + e.getMessage());
        }

        return null;
    }

    private static Bitmap rotateBitmap(
            Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.
        matrix.postRotate(rotationDegrees);

        // Mirror the image along the X or Y axis.
        matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
        Bitmap rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // Recycle the old bitmap if it has changed.
        if (rotatedBitmap != bitmap) {
            bitmap.recycle();
        }
        return rotatedBitmap;
    }
}
