package com.arun.aiplayground

import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class TranslationLogic(
    private val detectedText: (String) -> Unit
) {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val languageIdentifier = LanguageIdentification.getClient()

    fun processImage(image: InputImage, onComplete: () -> Unit) {
        textRecognizer.process(image)
            .continueWithTask { task ->
                val resultText = task.result?.text ?: ""
                // Identify the language of the detected text
                detectedText(resultText)
                languageIdentifier.identifyLanguage(resultText)
            }
            .addOnSuccessListener { languageCode ->
                if (languageCode != "und") {
                    Log.d("AI_NLP", "Detected Language: $languageCode")
                    // Trigger translation or UI update
                } else {
                    Log.d("AI_NLP", "Language undetectable.")
                }
            }
            .addOnCompleteListener {
                // CRITICAL: Always release the frame when the ML pipeline finishes
                onComplete()
            }
    }
}