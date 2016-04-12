package com.coffree.gradualcamera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CameraActivity : AppCompatActivity() {

    private val TAG = "CameraActivity"

    private var cameraPreview: View? = null
    private var mControlsView: View? = null

    private var camera: Camera? = null
    private var preview: GradualPreview? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)

        mControlsView = findViewById(R.id.camera_controls)
        cameraPreview = findViewById(R.id.camera_preview)
        cameraPreview!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    override fun onResume() {
        super.onResume()
        val c = Camera.open()
        if (c != null) {
            camera = c
            preview = GradualPreview(this, c)
            val framePreview = findViewById(R.id.camera_preview) as FrameLayout
            framePreview.addView(preview)
            var params = c.parameters
            params.previewFormat = ImageFormat.NV21 // let's just make sure, even though it's the default
            val maxPreviewSize = params.supportedPreviewSizes.maxBy { it.width }
            if (maxPreviewSize != null) {
                params.setPreviewSize(maxPreviewSize.width, maxPreviewSize.height)
            }
            val focusModes = params.supportedFocusModes
            params.focusMode = listOf<String>(
                    Camera.Parameters.FOCUS_MODE_EDOF,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            ).find { it in params.supportedFocusModes } ?: params.focusMode
            val ps = params.previewSize
            Log.d(TAG, "preview size ${ps.width}×${ps.height}, focus mode ${params.focusMode}")
            c.parameters = params
        }
    }

    override fun onPause() {
        super.onPause()
        val framePreview = findViewById(R.id.camera_preview) as FrameLayout
        framePreview.removeView(preview)
        camera?.release()
        Log.d(TAG, "camera released")
    }

}
