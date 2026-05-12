package com.arun.aiplayground

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

class PersonaOverlay(context: Context, attrs: AttributeSet): View(context, attrs) {
    private var maskBitmap: Bitmap? = null
    private var cameraFrame: Bitmap? = null
    private var rotationDegrees: Int = 0
    private var backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.office_bg)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // SRC_IN: The source (camera frame) is drawn only where it overlaps
        // the destination (the mask).
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.OUTER)
    }

    // Call this from the Activity when a new mask is ready
    fun updateMask(newMask: Bitmap) {
        this.maskBitmap = newMask
        postInvalidate()    // Re-draw on the next frame
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun updateData(newMask: Bitmap, newFrame: Bitmap, rotation: Int) {
        Log.d("ML_DEBUG", "View received mask")
        this.maskBitmap = newMask
        this.cameraFrame = newFrame
        this.rotationDegrees = rotation
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mask = maskBitmap ?: return
        val frame = cameraFrame ?: return
        val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // 1. Draw the Background (The Office)
        canvas.drawBitmap(backgroundBitmap, null, viewRect, paint)

        // 2. Setup Masking Layer
        // We use a saveLayer so the SRC_IN mode only applies to this specific stack
        val sc = canvas.saveLayer(viewRect, null)

        // 3. Mirror the coordinate system (Senior Fix for Front Camera)
        canvas.save()
        canvas.scale(-1f, 1f, width / 2f, height / 2f)

        // 4. Draw the Mask (This is the DST)
        // It is a white silhouette of the person
        canvas.drawBitmap(mask, null, viewRect, paint)

        // 5. Draw the Camera Frame (This is the SRC)
        // It will ONLY show up where the Mask was white
        canvas.drawBitmap(frame, null, viewRect, maskPaint)

        canvas.restore() // Undo mirror
        canvas.restoreToCount(sc) // Apply the PorterDuff blend

        // 6. Optional: Draw a Cyan Aura around the mask
         canvas.drawBitmap(mask, null, viewRect, auraPaint)
    }

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        val mask = maskBitmap ?: return
//        val frame = cameraFrame ?: return
//        val viewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
//
//        // 1. Draw the Background (Static, no rotation needed)
//        canvas.drawBitmap(backgroundBitmap, null, viewRect, paint)
//
//        // 2. Create the MASK Matrix
//        val maskMatrix = Matrix().apply {
//            // Center the 256x256 mask at (0,0)
//            postTranslate(-mask.width / 2f, -mask.height / 2f)
//            // Rotate
//            postRotate((rotationDegrees + 90).toFloat())
//            // Mirror for Front Camera
//            postScale(-1f, 1f)
//            // Scale to View Size (Handling rotation swap)
////            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
////            val maskTargetW = if (isRotated) mask.height else mask.width
////            val maskTargetH = if (isRotated) mask.width else mask.height
////            postScale(width / maskTargetW.toFloat(), height / maskTargetH.toFloat())
//
//            // FIX THE SHRINKING: Use the 'Transposed' dimensions
//            // Since we rotated 90, the mask's 'width' is now its 'height'
//            val rotatedMaskWidth = mask.height.toFloat()
//            val rotatedMaskHeight = mask.width.toFloat()
//
//            // Center Crop Logic: Scale to the larger dimension to fill the screen
//            val scale = Math.max(
//                width / rotatedMaskWidth,
//                height / rotatedMaskHeight
//            )
//            postScale(scale, scale)
//            // Move to View Center
//            postTranslate(width / 2f, height / 2f)
//        }
//
//        // 3. Create the FRAME Matrix (Same logic, but using frame dimensions)
//        val frameMatrix = Matrix().apply {
//            postTranslate(-frame.width / 2f, -frame.height / 2f)
//            postRotate(rotationDegrees.toFloat())
//            postScale(-1f, 1f)
//            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
//            val frameTargetW = if (isRotated) frame.height else frame.width
//            val frameTargetH = if (isRotated) frame.width else frame.height
//            postScale(width / frameTargetW.toFloat(), height / frameTargetH.toFloat())
//            postTranslate(width / 2f, height / 2f)
//        }
//
//        // 4. Composite the Mask and Frame
//        val sc = canvas.saveLayer(viewRect, null)
//
//        // Draw Mask first (Destination)
//        // IMPORTANT: Use the SAME matrix
//        canvas.drawBitmap(mask, maskMatrix, paint)
//
//        // Draw Frame second (Source) with the SRC_IN mode
//        // Because they use the same matrix, they overlap perfectly
//        canvas.drawBitmap(frame, frameMatrix, maskPaint)
//
//        canvas.restoreToCount(sc)
//    }
}