package com.coffree.gradualcamera

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

/**
 * Created by fraser on 09/04/16.
 */
class GradualPreview(context: Context, var camera: Camera, var preferredAspect: Float) : SurfaceView(context), SurfaceHolder.Callback {
    init {
        holder.addCallback(this)
    }

    val TAG = "GradualPreview"

    override fun surfaceCreated(holder: SurfaceHolder?) {
        startPreview()
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        stopPreview()
        startPreview()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        stopPreview()
        // Take care of releasing the Camera preview in your activity.
    }

    override protected fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var w = View.MeasureSpec.getSize(widthMeasureSpec)
        var h = View.MeasureSpec.getSize(heightMeasureSpec)
        val aspect = w.toFloat() / h.toFloat()
        Log.d(TAG, "onMeasure(${w}×${h}) aspect ${aspect} preferred aspect ${preferredAspect}")
        if (aspect > preferredAspect) {
            // available space has a wider aspect ratio than the preferred preview, constraint our width to match the preview
            w = (h.toFloat() * preferredAspect).toInt()
        } else if (aspect < preferredAspect) {
            // available space has narrower aspect ratio then preferred preview, constrain our height
            h = (w.toFloat() / preferredAspect).toInt()
        }
        Log.d(TAG, "selected ${w}×${h}")
        setMeasuredDimension(w, h)
    }

    private fun startPreview() {
        if (holder!!.surface != null) {
            try {
                camera.setPreviewDisplay(holder)
                camera.startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: " + e.message)
            }
        }
    }

    private fun stopPreview() {
        try {
            camera.setPreviewCallback(null)
            camera.stopPreview()
        } catch (e: Exception) {
            // do nothing, no preview exists
        }
    }
}