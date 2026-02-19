package com.arun.aiplayground

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

class TranslationAnalyzer(
    private val translationLogic: TranslationLogic
): ImageAnalysis.Analyzer {

    // Concurrency lock
    private var isProcessing = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        // The Edge Constraint: Drop frames if the processor is busy to save battery
        if (isProcessing || imageProxy.image == null) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val inputImage = InputImage.fromMediaImage(
            imageProxy.image!!,
            imageProxy.imageInfo.rotationDegrees
        )

        translationLogic.processImage(inputImage) {
            // Unlock and free the memory
            imageProxy.close()
            isProcessing = false
        }
    }
}