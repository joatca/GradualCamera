package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class LeftRightForger(increment: Int, blurMultiple: Int, target: Bitmap?) : SequentialForger(increment, blurMultiple, target) {

    override fun calcInitialPosition(): Int {
        return 0
    }

    override fun calcFinalPosition(): Int {
        return target?.width ?: 0
    }

    override fun getNextPosition(): Int {
        return position + increment
    }

    override fun getBlendPosition(): Int {
        return position - increment*blurMultiple
    }

    override fun getLinePosition(pos: Int): Int {
        return pos + 2
    }

    override fun atEnd(): Boolean {
        return position >= finalPosition
    }

    override fun blendShader(from: Float, to: Float): Shader {
        return LinearGradient(from.toFloat(), 0f, to.toFloat(), 0f, Color.argb(0,0,0,0), Color.argb(255,0,0,0), Shader.TileMode.CLAMP)
    }

    override fun drawFramePiece(canvas: Canvas, from: Float, to: Float, paint: Paint) {
        if (target != null) {
            canvas.drawRect(from, 0f, to, target.height.toFloat(), paint)
        }
    }

    override fun drawProgressLine(canvas: Canvas, pos: Float, paint: Paint) {
        if (target != null) {
            canvas.drawLine(pos, 0f, pos, target.height.toFloat(), paint)
        }
    }
}
