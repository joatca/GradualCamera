
package com.coffree.gradualcamera

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.renderscript.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CameraActivity : AppCompatActivity() {

    private val TAG = "CameraActivity"

    private var cameraPreview: View? = null
    private var picturePreview: ImageView? = null
    private var previewSize: Camera.Size? = null

    private var camera: Camera? = null
    private var preview: GradualPreview? = null

    private var imageBitmap: Bitmap? = null

    private var frameBuffer: ByteArray? = null
    private var frameBitmap: Bitmap? = null
    private var startTime: Long = 0
    private var frameCount: Long = 0

    private var renderScript: RenderScript? = null
    private var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    //var yuvType: Type.Builder? = null
    private var allocIn: Allocation? = null
    //var rgbaType: Type.Builder? = null
    private var allocOut: Allocation? = null

    private var forger: SequentialForger? = null

    private var mode: Mode = Mode.CENTRE_OUT

    private var modeMenu: LinearLayout? = null

    private var startPicture: ImageButton? = null
    private var modeButton: ImageButton? = null
    private var speedButton: ImageButton? = null

    enum class Mode {
        LEFT_RIGHT, RIGHT_LEFT,
        TOP_DOWN, BOTTOM_UP,
        CENTRE_IN, CENTRE_OUT
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)

        picturePreview = findViewById(R.id.picture_preview) as ImageView
        cameraPreview = findViewById(R.id.camera_preview)
        cameraPreview!!.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        modeMenu = findViewById(R.id.mode_menu) as LinearLayout
        startPicture = findViewById(R.id.start_picture) as ImageButton
        startPicture?.setOnClickListener { startPicture() }

        modeButton = findViewById(R.id.mode) as ImageButton
        modeButton?.setOnClickListener { showModeMenu() }

        mapOf(R.id.left_right_button to Mode.LEFT_RIGHT,
                R.id.right_left_button to Mode.RIGHT_LEFT,
                R.id.top_down_button to Mode.TOP_DOWN,
                R.id.bottom_up_button to Mode.BOTTOM_UP,
                R.id.centre_out_button to Mode.CENTRE_OUT,
                R.id.centre_in_button to Mode.CENTRE_IN).forEach {
            val (id, mode) = it
            val button = findViewById(id)
            button?.setOnClickListener { setMode(mode) }
        }

        disableAllButtons()

        setup()
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
            params.setPreviewSize(previewSize!!.width, previewSize!!.height)
            val focusModes = params.supportedFocusModes
            params.focusMode = listOf<String>(
                    Camera.Parameters.FOCUS_MODE_EDOF,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                    Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            ).find { it in params.supportedFocusModes } ?: params.focusMode
            params.videoStabilization = true
            val ps = params.previewSize
            Log.d(TAG, "preview size ${ps.width}×${ps.height}, focus mode ${params.focusMode}")
            c.parameters = params
            enableAllButtons()
        }
    }

    override fun onPause() {
        super.onPause()
        disableAllButtons()
        val framePreview = findViewById(R.id.camera_preview) as FrameLayout
        framePreview.removeView(preview)
        camera?.setPreviewCallbackWithBuffer(null)
        camera?.release()
        Log.d(TAG, "camera released")
    }

    fun setup() {
        val c = Camera.open()
        val params = c.parameters
        if (params != null) {
            previewSize = params.supportedPreviewSizes.maxBy { it.width } ?: params.previewSize
            val dimensions = previewSize!! // if prev line is null then camera is *soooooo* broken...
            val format = params.previewFormat
            frameBuffer = ByteArray(dimensions.width * dimensions.height * ImageFormat.getBitsPerPixel(format))
            val buf = frameBuffer
            if (buf != null) {
                imageBitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
                picturePreview?.setImageBitmap(imageBitmap)
                frameBitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
                renderScript = RenderScript.create(this)
                yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript))
                val yuvType = Type.Builder(renderScript, Element.U8(renderScript)).setX(buf.size)
                allocIn = Allocation.createTyped(renderScript, yuvType.create(), Allocation.USAGE_SCRIPT)
                val rgbaType = Type.Builder(renderScript, Element.RGBA_8888(renderScript)).setX(dimensions.width).setY(dimensions.height)
                allocOut = Allocation.createTyped(renderScript, rgbaType.create(), Allocation.USAGE_SCRIPT)
                startTime = System.currentTimeMillis()
                frameCount = 0
                Log.d(TAG, "buffers allocated")
            }
        }
        c.release()
    }

    fun startPicture() {
        hideAllMenus()
        enableOnlyStartButton()
        camera?.addCallbackBuffer(frameBuffer)
        val inc = 5
        forger = modeForger(mode, 5, imageBitmap)
        imageBitmap?.eraseColor(Color.TRANSPARENT)
        picturePreview?.invalidate()
        camera?.setPreviewCallbackWithBuffer { nv21: ByteArray, camera: Camera ->
            val bm = frameBitmap
            if (bm != null) {
                //Log.d(TAG, "raw frame length ${nv21.size}")
                val before = System.currentTimeMillis()
                allocIn?.copyFrom(nv21)
                yuvToRgbIntrinsic?.setInput(allocIn)
                yuvToRgbIntrinsic?.forEach(allocOut)
                allocOut?.copyTo(bm)
                if (forger?.update(bm) ?: false) {
                    camera.setPreviewCallbackWithBuffer(null)
                    enableAllButtons()
                } else {
                    ++frameCount
                    //Log.d(TAG, "this frame ${System.currentTimeMillis() - before} milliseconds, ${1000 * frameCount / (System.currentTimeMillis() - startTime)} preview frames/second")
                    camera.addCallbackBuffer(frameBuffer)
                }
                picturePreview?.invalidate() // force bitmap redraw
            }
        }
    }

    fun modeIcon(m: Mode): Int {
        return when (m) {
            Mode.LEFT_RIGHT -> R.drawable.ic_left_right
            Mode.RIGHT_LEFT -> R.drawable.ic_right_left
            Mode.TOP_DOWN -> R.drawable.ic_top_down
            Mode.BOTTOM_UP -> R.drawable.ic_bottom_up
            Mode.CENTRE_OUT -> R.drawable.ic_centre_out
            Mode.CENTRE_IN -> R.drawable.ic_centre_in
        }
    }

    fun modeIconLarge(m: Mode): Int {
        return when (m) {
            Mode.LEFT_RIGHT -> R.drawable.ic_left_right_large
            Mode.RIGHT_LEFT -> R.drawable.ic_right_left_large
            Mode.TOP_DOWN -> R.drawable.ic_top_down_large
            Mode.BOTTOM_UP -> R.drawable.ic_bottom_up_large
            Mode.CENTRE_OUT -> R.drawable.ic_centre_out_large
            Mode.CENTRE_IN -> R.drawable.ic_centre_in_large
        }
    }

    fun modeForger(m: Mode, speed: Int, bm: Bitmap?): SequentialForger {
        return when (m) {
            Mode.LEFT_RIGHT -> LeftRightForger(speed, 1, bm)
            Mode.RIGHT_LEFT -> LeftRightForger(-speed, 1, bm)
            Mode.TOP_DOWN -> TopBottomForger(speed, 1, bm)
            Mode.BOTTOM_UP -> TopBottomForger(-speed, 1, bm)
            Mode.CENTRE_OUT -> CentreCircleForger(speed, 1, bm)
            Mode.CENTRE_IN -> CentreCircleForger(-speed, 1, bm)
        }
    }

    fun setAllButtons(e: Boolean) {
        arrayOf(startPicture, modeButton).forEach {
            it?.setEnabled(e)
        }
    }

    fun disableAllButtons() {
        setAllButtons(false)
    }

    fun enableAllButtons() {
        setAllButtons(true)
    }

    fun enableOnlyStartButton() {
        disableAllButtons()
        startPicture?.setEnabled(true)
    }

    fun hideAllMenus() {
        modeMenu?.visibility = View.INVISIBLE
        enableAllButtons()
    }

    fun showModeMenu() {
        hideAllMenus()
        modeMenu?.visibility = View.VISIBLE
        modeButton?.setEnabled(false)
    }

    fun setMode(m: Mode) {
        mode = m
        modeButton?.setImageResource(modeIcon(mode))
        hideAllMenus()
    }
}
