package com.coffree.gradualcamera

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

/**
 * Created by fraser on 09/04/16.
 */
class GradualPreview(context: Context, var camera: Camera) : SurfaceView(context), SurfaceHolder.Callback {
    init {
        holder.addCallback(this)
    }

    val TAG = "GradualPreview"

    override fun surfaceCreated(holder: SurfaceHolder?) {
        try {
            camera.setPreviewDisplay(holder)
            camera.startPreview()
        } catch (e: IOException) {
            Log.d(TAG, "Error setting camera preview: " + e.message);
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        if (holder!!.surface != null) {
            try {
                camera.stopPreview()
            } catch (e: Exception) {
                // do nothing, no preview exists
            }
            try {
                camera.setPreviewDisplay(holder)
                camera.startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: " + e.message);
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        // Take care of releasing the Camera preview in your activity.
    }
}