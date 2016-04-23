package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class LongExposureForger(val blendAlpha: Int, target: Bitmap?) : Forger(target) {

    open val TAG = "LongExposureForger"

    var doBlend = false

    /* takes a new frame and composites it into the current bitmap somehow
        returns true if we are done, otherwise false
     */
    override fun update(frame: Bitmap): Boolean {
        if (target == null) {
            return false;
        }
        // now create a paint with shader based on the new frame
        val forgePaint = Paint()
        forgePaint.style = Paint.Style.FILL
        forgePaint.shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        forgePaint.alpha = if (doBlend) blendAlpha else 255
        // and finally paint the fade and the new strip onto the target
        targetCanvas.drawRect(0f, 0f, targetCanvas.width.toFloat(), targetCanvas.height.toFloat(), forgePaint)
        doBlend = true
        return false
    }

    override fun terminates(): Boolean {
        return false
    }
}
