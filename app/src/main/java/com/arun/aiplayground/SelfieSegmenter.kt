package com.arun.aiplayground

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import androidx.core.graphics.createBitmap
import java.util.concurrent.Executors

class SelfieSegmenter() {
    private lateinit var colors: IntArray
    private lateinit var btmap: Bitmap

    // 1. Configure for speed (Raw size is better for mobile NPUs)
    private val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
        .build()

    private val segmenter = Segmentation.getClient(options)

    fun processImage(image: InputImage, maskedBitmap: (Bitmap) -> Unit) {
        segmenter.process(image)
            .addOnSuccessListener(Executors.newSingleThreadExecutor()) { mask ->
                Log.d("ML_DEBUG", "2. ML Kit Success") // If you don't see this, the model is stuck
                val bitmap = maskToBitmap(mask)
                // Switch back to main to update UI
                Handler(Looper.getMainLooper()).post {
                    Log.d("ML_DEBUG", "2.5 Triggering callback to Activity")
                    maskedBitmap(bitmap)
                }
            }
            .addOnFailureListener { e ->
                // This will tell you if the ML model itself is crashing
                Log.e("ML_DEBUG", "2. ML Kit Failure: ${e.message}")
            }
    }

    private fun maskToBitmap(mask: SegmentationMask): Bitmap {
        try {
            val maskBuffer = mask.buffer
            val maskWidth = mask.width
            val maskHeight = mask.height

            // REUSE: Only create if null or size changed
            if (!::btmap.isInitialized || btmap.width != maskWidth || btmap.height != maskHeight) {
                btmap = createBitmap(maskWidth, maskHeight)
                colors = IntArray(maskWidth * maskHeight)
            }

            maskBuffer.rewind()

            // DO NOT call getFloat() here for logging, it consumes the first pixel!
            //Log.d("TAG", "Confidence - ${maskBuffer.getFloat()}")
            for (i in 0 until maskWidth * maskHeight) {
                val confidence = maskBuffer.getFloat()
                // To create a mask for PORTER_DUFF,
                // the person MUST be solid (Alpha 255) and background MUST be transparent (Alpha 0)
                if (confidence > 0.5f) {
                    colors[i] = Color.WHITE // Person = Opaque
                } else {
                    colors[i] = Color.TRANSPARENT // Background = See-through
                }
            }
            btmap.setPixels(colors, 0, maskWidth, 0, 0, maskWidth, maskHeight)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("ML_DEBUG", "2.5 Mask to bitmap error")
        }
        return btmap
    }
}