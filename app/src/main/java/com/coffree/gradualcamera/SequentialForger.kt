package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
abstract class SequentialForger(val increment: Int, val blurMultiple: Int, val target: Bitmap?) {

    open val TAG = "LeftRightForger"
    val targetCanvas = Canvas(target)
    open var position: Int = calcInitialPosition()
    open val finalPosition: Int = calcFinalPosition()

    fun getNextPosition(): Int = position + increment
    fun getBlendPosition(): Int = position - increment*blurMultiple
    fun getLinePosition(pos: Int): Int = pos + 2 * (increment / Math.abs(increment))
    fun atEnd(): Boolean = if (increment > 0) position >= finalPosition else position <= finalPosition

    abstract fun calcInitialPosition(): Int
    abstract fun calcFinalPosition(): Int
    abstract fun blendShader(from: Float, to: Float): Shader
    abstract fun drawFramePiece(canvas: Canvas, from: Float, to: Float, paint: Paint)
    abstract fun drawProgressLine(canvas: Canvas, pos: Float, paint: Paint)

    /* takes a new frame and composites it into the current bitmap somehow
        returns true if we are done, otherwise false
     */
    fun update(frame: Bitmap): Boolean {
        if (target == null) {
            return false;
        }
        // compute the proportional distance across the bitmap as the proportion of the current time to the duration
        var newPosition = getNextPosition()
        Log.d(TAG, "position ${position} newPosition ${newPosition} finalPosition ${finalPosition}")
        if (newPosition == position) {
            return false; // this frame hasn't advanced us any, just ignore it - this is unlikely!
        }
        var done = false
        if (atEnd()) {
            newPosition = finalPosition // don't go further than the end
            done = true
        }
        val blendPosition = getBlendPosition()
        // make a paint that will apply a fade-in alpha before the new position that can be overlaid on the previous strip
        val blendPaint = Paint()
        blendPaint.shader = blendShader(blendPosition.toFloat(), position.toFloat())
        blendPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        // apply the alpha to the new frame
        val frameCanvas = Canvas(frame)
        drawFramePiece(frameCanvas, blendPosition.toFloat(), position.toFloat(), blendPaint)
        // now create a paint with shader based on the new frame
        val forgePaint = Paint()
        forgePaint.style = Paint.Style.FILL
        forgePaint.shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        // and finally paint the fade and the new strip onto the target
        drawFramePiece(targetCanvas, blendPosition.toFloat(), newPosition.toFloat(), forgePaint)
        val linePaint = Paint()
        linePaint.style = Paint.Style.STROKE
        linePaint.color = Color.GREEN
        linePaint.strokeWidth = 2f
        drawProgressLine(targetCanvas, getLinePosition(newPosition).toFloat(), linePaint)
        position = newPosition
        return done
    }
}
