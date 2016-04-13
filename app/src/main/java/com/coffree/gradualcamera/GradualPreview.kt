package com.coffree.gradualcamera

import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Created by fraser on 09/04/16.
 */
class GradualPreview(var activity: CameraActivity, var camera: Camera) : SurfaceView(activity), SurfaceHolder.Callback {
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

    private fun startPreview() {
        if (holder!!.surface != null) {
            try {
                camera.setPreviewDisplay(holder)
                camera.setPreviewCallbackWithBuffer(activity)
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