package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class LeftRightForger(val durationMillis: Long, val target: Bitmap?) {

    val TAG = "LeftRightForger"
    val startTime by lazy { System.currentTimeMillis() }
    val endTime by lazy { startTime + durationMillis }
    val canvas = Canvas(target)
    var prevPosition: Int = 0

    /* takes a new frame and composites it into the current bitmap somehow
        returns true if we are done, otherwise false
     */
    fun update(frame: Bitmap): Boolean {
        if (target == null) {
            return false;
        }
        val now = System.currentTimeMillis()
        // compute the proportional distance across the bitmap as the proportion of the current time to the duration
        var newPosition = ((now - startTime) * canvas.width / durationMillis).toInt()
        if (newPosition == prevPosition) {
            return false; // this frame hasn't advanced us any, just ignore it - this is unlikely!
        }
        var done = false
        if (newPosition >= target.width) {
            newPosition = target.width - 1 // don't go further than the end
            done = true
        }
        val forgePaint = Paint()
        forgePaint.style = Paint.Style.FILL
        forgePaint.shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawRect(prevPosition.toFloat(), 0f, newPosition.toFloat(), (target.height - 1).toFloat(), forgePaint)
        val linePaint = Paint()
        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.WHITE
        linePaint.strokeWidth = 1f
        canvas.drawLine(newPosition.toFloat(), 0f, newPosition.toFloat(), target.height.toFloat(), linePaint)
        Log.d(TAG, "draw from ${prevPosition} to ${newPosition}")
        prevPosition = newPosition
        return done
    }
}
