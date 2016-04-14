package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class LeftRightForger(val increment: Int, val blurMultiple: Float, val target: Bitmap?) {

    val TAG = "LeftRightForger"
    val targetCanvas = Canvas(target)
    var position: Int = 0

    /* takes a new frame and composites it into the current bitmap somehow
        returns true if we are done, otherwise false
     */
    fun update(frame: Bitmap): Boolean {
        if (target == null) {
            return false;
        }
        // compute the proportional distance across the bitmap as the proportion of the current time to the duration
        var newPosition = position + increment
        if (newPosition == position) {
            return false; // this frame hasn't advanced us any, just ignore it - this is unlikely!
        }
        var done = false
        if (newPosition >= target.width) {
            newPosition = target.width - 1 // don't go further than the end
            done = true
        }
        val blendPosition = position - increment*blurMultiple
        // make a paint that will apply a fade-in alpha before the new position that can be overlaid on the previous strip
        val blendPaint = Paint()
        blendPaint.shader = LinearGradient(blendPosition.toFloat(), 0f, position.toFloat(), 0f, Color.argb(0,0,0,0), Color.argb(255,0,0,0), Shader.TileMode.CLAMP)
        blendPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        // apply the alpha to the new frame
        val frameCanvas = Canvas(frame)
        frameCanvas.drawRect(blendPosition.toFloat(), 0f, position.toFloat(), (target.height.toFloat()), blendPaint)
        // now create a paint with shader based on the new frame
        val forgePaint = Paint()
        forgePaint.style = Paint.Style.FILL
        forgePaint.shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        // and finally paint the fade and the new strip onto the target
        targetCanvas.drawRect(blendPosition.toFloat(), 0f, newPosition.toFloat(), (target.height - 1).toFloat(), forgePaint)
        val linePaint = Paint()
        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.GREEN
        linePaint.strokeWidth = 2f
        targetCanvas.drawLine((newPosition+2).toFloat(), 0f, (newPosition+2).toFloat(), target.height.toFloat(), linePaint)
        position = newPosition
        return done
    }
}
