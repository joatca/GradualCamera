package com.coffree.gradualcamera

import android.graphics.*

/**
 * Created by fraser on 11/04/16.
 */
class LeftRightForger(val durationMillis: Long, val target: Bitmap) {

    val startTime by lazy { System.currentTimeMillis() }
    val endTime by lazy { startTime + durationMillis }
    val canvas = Canvas(target)
    var prevPosition: Int = 0

    /* takes a new frame and composites it into the current bitmap somehow
        returns true if we are done, otherwise false
     */
    fun update(frame: Bitmap): Boolean {
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
        val paint = Paint()
        paint.style = Paint.Style.FILL
        paint.shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawRect(prevPosition.toFloat(), 0.0f, newPosition.toFloat(), (target.height - 1).toFloat(), paint)
        prevPosition = newPosition
        return done
    }
}
