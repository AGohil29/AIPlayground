package com.arun.aiplayground

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera() // Permission granted!
        } else {
            // Permission denied. Show a message or disable camera features.
            Toast.makeText(this, "Camera permission is required to detect objects.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewFinder = findViewById(R.id.viewFinder)

        requestPermissions()
    }

    private fun requestPermissions() {
        when {
            allPermissionsGranted() -> {
                startCamera()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                showRationaleDialog()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Camera Permission Needed")
            .setMessage("This app uses the camera to identify spending categories from your receipts. Please grant access.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startCamera() {
        // 1. Initialize the Camera Provider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 2. Setup Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            // 3. Setup Image Analysis (The code you provided)
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
//                    it.setAnalyzer(ContextCompat.getMainExecutor(this), ObjectAnalyzer { bitmaps ->
//                        if (bitmaps.isNotEmpty()) {
//                            // Update your Compose UI or Graphic Overlay here
//                            findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmaps[0])
//                            Log.d("CameraX", "Image received")
//                        }
//                    })
                    // text analyzer
                    it.setAnalyzer(
                        ContextCompat.getMainExecutor(this),
                        TranslationAnalyzer(TranslationLogic { value ->
                            Log.d("TAG", "Detected text: $value")
                        })
                    )
                }

            // 4. Bind to Lifecycle
            try {
                cameraProvider.unbindAll()   // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(
                    this, // The LifecycleOwner (MainActivity)
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraX", "Binding failed", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}