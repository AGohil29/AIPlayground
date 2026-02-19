package com.arun.aiplayground

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class ObjectAnalyzer(
    private val onObjectDetected: (List<Bitmap>) -> Unit
): ImageAnalysis.Analyzer {
    // 1. Configure the "Brain" for Streaming
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE) // Fast & Low Latency
        .enableClassification() // Tells you if it's "Food", "Fashion", etc.
        .build()

    private val detector = ObjectDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // 2. Wrap the image with the correct rotation metadata
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            detector.process(image)
                .addOnSuccessListener { objects ->
                    val croppedBitmaps = objects.mapNotNull { obj ->
                        imageProxy.toCroppedBitmap(obj.boundingBox)
                    }
                    onObjectDetected(croppedBitmaps)
                }
                .addOnCompleteListener {
                    // 3. CRITICAL: Always close the proxy to free up the buffer!
                    imageProxy.close()
                }
        }
    }
}

// Helper to convert ImageProxy to cropped Bitmap
fun ImageProxy.toCroppedBitmap(boundingBox: Rect): Bitmap? {
    val bitmap = this.toBitmap() // Extension function available in CameraX 1.3+

    // Ensure the crop area is within the actual bitmap bounds to prevent crashes
    val left = boundingBox.left.coerceAtLeast(0)
    val top = boundingBox.top.coerceAtLeast(0)
    val width = boundingBox.width().coerceAtMost(bitmap.width - left)
    val height = boundingBox.height().coerceAtMost(bitmap.height - top)

    return if (width > 0 && height > 0) {
        Bitmap.createBitmap(bitmap, left, top, width, height)
    } else null
}