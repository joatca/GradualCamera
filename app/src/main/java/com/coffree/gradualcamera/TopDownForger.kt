package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class TopDownForger(increment: Int, blurMultiple: Float, target: Bitmap?) : SequentialForger(increment, blurMultiple, target) {

    override fun calcInitialPosition(): Int {
        return 0
    }

    override fun calcFinalPosition(): Int {
        return target?.height ?: 0
    }

    override fun getNextPosition(): Int {
        return position + increment
    }

    override fun atEnd(): Boolean {
        return position >= finalPosition
    }

    override fun blendShader(from: Float, to: Float): Shader {
        return LinearGradient(0f, from.toFloat(), 0f, to.toFloat(), Color.argb(0,0,0,0), Color.argb(255,0,0,0), Shader.TileMode.CLAMP)
    }

    override fun drawFramePiece(canvas: Canvas, from: Float, to: Float, paint: Paint) {
        if (target != null) {
            canvas.drawRect(0f, from, target.width.toFloat(), to, paint)
        }
    }

    override fun drawProgressLine(canvas: Canvas, pos: Float, paint: Paint) {
        if (target != null) {
            canvas.drawLine(0f, pos, target.width.toFloat(), pos, paint)
        }
    }
}
