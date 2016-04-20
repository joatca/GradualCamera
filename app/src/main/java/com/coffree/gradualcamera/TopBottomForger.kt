package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class TopBottomForger(increment: Int, blurMultiple: Int, target: Bitmap?) : SequentialForger(increment, blurMultiple, target) {

    override fun calcInitialPosition(): Int = if (increment > 0) 0 else target?.height ?: 0

    override fun calcFinalPosition(): Int = if (increment > 0) target?.height ?: 0 else 0

    override fun blendShader(from: Float, to: Float): Shader {
        return LinearGradient(0f, from.toFloat(), 0f, to.toFloat(), Color.argb(0,0,0,0), Color.argb(255,0,0,0), Shader.TileMode.CLAMP)
    }

    override fun drawFramePiece(canvas: Canvas, from: Float, to: Float, paint: Paint) {
        if (target != null) {
            canvas.drawRect(0f, from, target.width.toFloat(), to, paint)
        }
    }
}
