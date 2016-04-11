package com.coffree.gradualcamera

import android.annotation.SuppressLint
import android.content.Context
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

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CameraActivity : AppCompatActivity() {


    private val TAG = "CameraActivity"

    private val mHideHandler = Handler()
    private var cameraPreview: View? = null
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        cameraPreview!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private var mControlsView: View? = null
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        val actionBar = supportActionBar
        actionBar?.show()
        mControlsView!!.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val mDelayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        if (AUTO_HIDE) {
            delayedHide(AUTO_HIDE_DELAY_MILLIS)
        }
        false
    }

    private var camera: Camera? = null
    private var preview: GradualPreview? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)

        mVisible = true
        mControlsView = findViewById(R.id.fullscreen_content_controls)
        cameraPreview = findViewById(R.id.camera_preview)


        // Set up the user interaction to manually show or hide the system UI.
        cameraPreview!!.setOnClickListener { toggle() }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button)!!.setOnTouchListener(mDelayHideTouchListener)

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
            params.previewFormat = ImageFormat.NV21 // let's just make sure
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

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        val actionBar = supportActionBar
        actionBar?.hide()
        mControlsView!!.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    @SuppressLint("InlinedApi")
    private fun show() {
        // Show the system bar
        cameraPreview!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [.AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [.AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
