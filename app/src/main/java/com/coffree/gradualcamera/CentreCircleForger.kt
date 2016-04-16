package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
class CentreCircleForger(increment: Int, blurMultiple: Int, target: Bitmap?) : SequentialForger(increment, blurMultiple, target) {

    override val TAG = "CentreCircleForger"

    val maxRadius = if (target != null) Math.sqrt((target.width*target.width + target.height*target.height).toDouble()).toInt() / 2 else 0
    val centreX = (target?.width ?: 0) / 2
    val centreY = (target?.height ?: 0) / 2

    override var position: Int = calcInitialPosition()
    override val finalPosition: Int = calcFinalPosition()

    override fun calcInitialPosition(): Int =  if (increment > 0) 0 else maxRadius
    override fun calcFinalPosition(): Int = if (increment > 0) maxRadius else 0

    override fun blendShader(from: Float, to: Float): Shader {
        if (increment > 0) {
            return RadialGradient(centreX.toFloat(), centreY.toFloat(),
                    if (to > 0f) to.toFloat() else 1f,
                    intArrayOf(Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(255, 0, 0, 0)),
                    floatArrayOf(0f, from.toFloat(), to.toFloat()),
                    Shader.TileMode.CLAMP)
        } else {
            return RadialGradient(centreX.toFloat(), centreY.toFloat(),
                    if (to > 0f) to.toFloat() else 1f,
                    intArrayOf(Color.argb(255, 0, 0, 0), Color.argb(0, 0, 0, 0), Color.argb(0, 0, 0, 0)),
                    floatArrayOf(to.toFloat(), from.toFloat(), 0f),
                    Shader.TileMode.CLAMP)
        }
    }

    override fun drawFramePiece(canvas: Canvas, from: Float, to: Float, paint: Paint) {
        if (target != null && to >= increment) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = increment.toFloat()
            canvas.drawCircle(centreX.toFloat(), centreY.toFloat(), to.toFloat() - increment.toFloat() / 2f, paint)
        }
    }

    override fun drawProgressLine(canvas: Canvas, pos: Float, paint: Paint) {
        if (target != null && pos > 0) {
            canvas.drawCircle(centreX.toFloat(), centreY.toFloat(), pos.toFloat(), paint)
            //canvas.drawLine(pos, 0f, pos, target.height.toFloat(), paint)
        }
    }
}
