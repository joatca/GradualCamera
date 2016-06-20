
package com.coffree.gradualcamera

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.drawable.AnimationDrawable
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.renderscript.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CameraActivity : AppCompatActivity() {

    private val TAG = "CameraActivity"
    private val IMAGE_SUBDIR = "Gradual"

    private val CAMERA_ID = 0

    private val cameraPreview by lazy { findViewById(R.id.camera_preview) as FrameLayout }
    private val picturePreview by lazy { findViewById(R.id.picture_preview) as ImageView }
    private val modeMenu by lazy { findViewById(R.id.mode_menu) as LinearLayout }
    private val speedMenu by lazy { findViewById(R.id.speed_menu) as LinearLayout }
    private val startPicture by lazy { findViewById(R.id.start_picture) as ImageButton }
    private val modeButton by lazy { findViewById(R.id.mode) as ImageButton }
    private val speedButton by lazy { findViewById(R.id.speed) as ImageButton }

    private val renderScript by lazy { RenderScript.create(this) }
    private val yuvToRgbIntrinsic by lazy { ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript)) }

    private var previewSize: Camera.Size? = null

    private var camera: Camera? = null
    private var preview: GradualPreview? = null

    private var imageBitmap: Bitmap? = null

    private var frameBuffer: ByteArray? = null
    private var frameBitmap: Bitmap? = null

    //var yuvType: Type.Builder? = null
    private var allocIn: Allocation? = null
    //var rgbaType: Type.Builder? = null
    private var allocOut: Allocation? = null

    private var forger: Forger? = null

    private var mode = Mode.CENTRE_OUT
    private var speed = Speed.MEDIUM

    private var pictureRunning: Boolean = false

    enum class Mode {
        LEFT_RIGHT, RIGHT_LEFT,
        TOP_DOWN, BOTTOM_UP,
        CENTRE_IN, CENTRE_OUT,
        LONG_EXPOSURE
    }

    private val modeByButton = mapOf(
            R.id.left_right_button to Mode.LEFT_RIGHT,
            R.id.right_left_button to Mode.RIGHT_LEFT,
            R.id.top_down_button to Mode.TOP_DOWN,
            R.id.bottom_up_button to Mode.BOTTOM_UP,
            R.id.centre_out_button to Mode.CENTRE_OUT,
            R.id.centre_in_button to Mode.CENTRE_IN,
            R.id.long_exp_button to Mode.LONG_EXPOSURE
    )

    private val iconByMode = mapOf(
            Mode.LEFT_RIGHT to R.drawable.anim_left_right_large,
            Mode.RIGHT_LEFT to R.drawable.anim_right_left_large,
            Mode.TOP_DOWN to R.drawable.anim_top_down_large,
            Mode.BOTTOM_UP to R.drawable.anim_bottom_up_large,
            Mode.CENTRE_OUT to R.drawable.anim_centre_out_large,
            Mode.CENTRE_IN to R.drawable.anim_centre_in_large,
            Mode.LONG_EXPOSURE to R.drawable.anim_long_exp_large
    )

    enum class Speed {
        SLOW, MEDIUM, FAST
    }

    private val speedByButton = mapOf(
            R.id.slow_button to Speed.SLOW,
            R.id.medium_button to Speed.MEDIUM,
            R.id.fast_button to Speed.FAST
    )

    private val iconBySpeed = mapOf(
            Speed.SLOW to R.drawable.ic_slow,
            Speed.MEDIUM to R.drawable.ic_medium,
            Speed.FAST to R.drawable.ic_fast
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera)

        startPicture.setOnClickListener {
            if (pictureRunning) {
                if (!(forger?.terminates() ?: true)) {
                     saveImage(imageBitmap)
                }
            } else {
                startPicture()
            }
        }

        modeButton.setOnClickListener {
            if (pictureRunning) {
                stopPicture()
            } else {
                showModeMenu()
            }
        }

        modeByButton.forEach {
            val (id, mode) = it
            (findViewById(id) as ImageButton).setOnClickListener { setMode(mode) }
        }

        speedButton.setOnClickListener { showSpeedMenu() }

        speedByButton.forEach {
            val (id, speed) = it
            (findViewById(id) as ImageButton).setOnClickListener { setSpeed(speed) }
        }

        disableAllButtons()

        mode = try {
            Mode.valueOf(getPreferences(MODE_PRIVATE).getString("mode", Mode.CENTRE_OUT.toString()))
        }
        catch (e: IllegalArgumentException) {
            Mode.CENTRE_OUT
        }

        speed = try {
            Speed.valueOf(getPreferences(MODE_PRIVATE).getString("speed", Speed.MEDIUM.toString()))
        }
        catch (e: IllegalArgumentException) {
            Speed.MEDIUM
        }

        setMode(mode)
        setSpeed(speed)

    }

    override fun onResume() {
        super.onResume()
        cameraPreview.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        Camera.open(CAMERA_ID)?.let { c ->
            c.parameters?.let { params ->
                if (frameBuffer == null) {
                    picturePreview.let { pp ->
                        // have we done this before?
                        // our preview fills the screen so this is easier than a tree view observer
                        val point = Point()
                        windowManager.defaultDisplay.getRealSize(point)
                        var previewFrameAspect = point.x.toFloat() / point.y.toFloat()
                        Log.d(TAG, "aspect ${previewFrameAspect}")
                        val largestMatchingPreviewSize = params.supportedPreviewSizes.filter {
                            Math.abs(previewFrameAspect - it.width.toFloat() / it.height.toFloat()) < 0.1
                        }.maxBy { it.width }
                        val largestPreviewSize = params.supportedPreviewSizes.maxBy { it.width * it.height }
                        if (largestMatchingPreviewSize != null) {
                            Log.d(TAG, "max matching ${largestMatchingPreviewSize.width}×${largestMatchingPreviewSize.height}")
                        }
                        if (largestPreviewSize != null) {
                            Log.d(TAG, "max ${largestPreviewSize.width}×${largestPreviewSize.height}")
                        }
                        previewSize = largestMatchingPreviewSize ?: largestPreviewSize ?: params.previewSize
                        val dimensions = previewSize!! // if prev line is null then camera is *soooooo* broken...
                        val format = params.previewFormat
			// this renderscript stuff comes from http://stackoverflow.com/questions/1893072/getting-frames-from-video-image-in-android/36409748#36409748
                        frameBuffer = ByteArray(dimensions.width * dimensions.height * ImageFormat.getBitsPerPixel(format))
                        frameBuffer?.let { buf ->
                            imageBitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
                            picturePreview.setImageBitmap(imageBitmap)
                            frameBitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
                            val yuvType = Type.Builder(renderScript, Element.U8(renderScript)).setX(buf.size)
                            allocIn = Allocation.createTyped(renderScript, yuvType.create(), Allocation.USAGE_SCRIPT)
                            val rgbaType = Type.Builder(renderScript, Element.RGBA_8888(renderScript)).setX(dimensions.width).setY(dimensions.height)
                            allocOut = Allocation.createTyped(renderScript, rgbaType.create(), Allocation.USAGE_SCRIPT)
                            Log.d(TAG, "buffers allocated")
                        }
                    }
                }
                previewSize?.let { ps ->
                    camera = c
                    preview = GradualPreview(this, c, ps.width.toFloat() / ps.height.toFloat())
                    cameraPreview.addView(preview)
                    c.parameters.let { params ->
                        params.previewFormat = ImageFormat.NV21 // let's just make sure, even though it's the default
                        params.setPreviewSize(previewSize!!.width, previewSize!!.height)
                        val focusModes = params.supportedFocusModes
                        params.focusMode = listOf<String>(
                                Camera.Parameters.FOCUS_MODE_EDOF,
                                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                        ).find { it in params.supportedFocusModes } ?: params.focusMode
                        params.videoStabilization = true
                        c.parameters = params
                        // set camera orientation based on boilerplate from http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
                        val info = Camera.CameraInfo()
                        Camera.getCameraInfo(CAMERA_ID, info)
                        val rotation = windowManager.defaultDisplay.rotation
                        val degrees = when(rotation) {
                            Surface.ROTATION_0 -> 0
                            Surface.ROTATION_90 -> 90
                            Surface.ROTATION_180 -> 180
                            Surface.ROTATION_270 -> 270
                            else -> Surface.ROTATION_0
                        }
                        c.setDisplayOrientation((info.orientation - degrees + 360) % 360)
                    }
                    setReadyButtonMode()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        disableAllButtons()
        cameraPreview.removeView(preview)
        camera?.setPreviewCallbackWithBuffer(null)
        camera?.release()
        Log.d(TAG, "camera released")
    }

    fun startPicture() {
        hideAllMenus()
        setPictureRunningButtonMode()
        camera?.addCallbackBuffer(frameBuffer)
        forger = modeForger(mode, speed, imageBitmap)
        imageBitmap?.eraseColor(resources.getColor(R.color.forger_background))
        picturePreview.invalidate()
        camera?.setPreviewCallbackWithBuffer { nv21: ByteArray, camera: Camera ->
            frameBitmap?.let { bm ->
                allocIn?.copyFrom(nv21)
                yuvToRgbIntrinsic?.setInput(allocIn)
                yuvToRgbIntrinsic?.forEach(allocOut)
                allocOut?.copyTo(bm)
                if (forger?.update(bm) ?: false) {
                    // image completed, save it and show save animation
                    saveImage(imageBitmap)
                } else {
                    camera.addCallbackBuffer(frameBuffer)
                }
                picturePreview.invalidate() // force bitmap redraw
            }
        }
        pictureRunning = true
    }

    fun stopPicture() {
        camera?.setPreviewCallbackWithBuffer(null)
        setReadyButtonMode()
        imageBitmap?.eraseColor(Color.TRANSPARENT)
        picturePreview.invalidate()
        pictureRunning = false
    }

    fun saveImage(bm: Bitmap?) {
        if (bm != null) {
            camera?.setPreviewCallbackWithBuffer(null)
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date());
            val imageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), IMAGE_SUBDIR)

            if (imageDir.exists() || imageDir.mkdirs()) {
                val imagePath = "${imageDir.path}${File.separator}IMG_${timeStamp}.jpg"
                Log.d(TAG, "output path ${imagePath}")
                val out = FileOutputStream(imagePath)
                val succeeded = bm.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.close()
                if (succeeded) {
                    MediaScannerConnection.scanFile(this, arrayOf(imagePath), arrayOf<String?>(null), null)
                } else {
                    File(imagePath).delete()
                }
                val anim = if (succeeded) {
                    // save succeeded
                    AnimationUtils.loadAnimation(this, R.anim.image_saved)
                } else {
                    Toast.makeText(this, R.string.image_save_failed, Toast.LENGTH_LONG).show()
                    AnimationUtils.loadAnimation(this, R.anim.image_not_saved)
                }
                anim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                    override fun onAnimationStart(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        stopPicture()
                    }
                })
                picturePreview.startAnimation(anim)
            }
        }
    }

    fun modeIcon(m: Mode): Int {
        return iconByMode[m] ?: R.drawable.anim_centre_out_large
    }

    fun speedIcon(s: Speed): Int {
        return iconBySpeed[s] ?: R.drawable.ic_medium
    }

    fun modeForger(m: Mode, speed: Speed, bm: Bitmap?): Forger {
        return when (m) {
            Mode.LEFT_RIGHT -> LeftRightForger(speedToIncrement(speed), 2, bm)
            Mode.RIGHT_LEFT -> LeftRightForger(-speedToIncrement(speed), 2, bm)
            Mode.TOP_DOWN -> TopBottomForger(speedToIncrement(speed), 2, bm)
            Mode.BOTTOM_UP -> TopBottomForger(-speedToIncrement(speed), 2, bm)
            Mode.CENTRE_OUT -> CentreCircleForger(speedToIncrement(speed), 2, bm)
            Mode.CENTRE_IN -> CentreCircleForger(-speedToIncrement(speed), 2, bm)
            Mode.LONG_EXPOSURE -> LongExposureForger(speedToBlendAlpha(speed), bm)
        }
    }

    fun setAllButtons(e: Boolean) {
        arrayOf(startPicture, modeButton, speedButton).forEach {
            it.setEnabled(e)
        }
    }

    fun disableAllButtons() {
        setAllButtons(false)
    }

    fun enableAllButtons() {
        setAllButtons(true)
    }

    fun setReadyButtonMode() {
        enableAllButtons()
        startPicture.setImageResource(R.drawable.ic_camera_24px)
        setModeButton(mode)
    }

    fun setPictureRunningButtonMode() {
        modeButton.setImageResource(R.drawable.ic_cancel_white_48dp)
    }

    fun hideAllMenus() {
        modeMenu.visibility = View.INVISIBLE
        modeByButton.keys.forEach {
            val b = findViewById(it) as ImageButton
            (b.drawable as AnimationDrawable).stop()
        }
        speedMenu.visibility = View.INVISIBLE
        setReadyButtonMode()
    }

    fun showModeMenu() {
        hideAllMenus()
        modeMenu.visibility = View.VISIBLE
        modeButton.setEnabled(false)
        modeByButton.keys.forEach {
            ((findViewById(it) as ImageButton).drawable as AnimationDrawable).start()
        }
    }

    fun showSpeedMenu() {
        hideAllMenus()
        speedMenu.visibility = View.VISIBLE
        speedButton.setEnabled(false)
    }

    fun setMode(m: Mode) {
        mode = m
        setPref("mode", mode.toString())
        setModeButton(m)
        hideAllMenus()
    }

    fun setSpeed(s: Speed) {
        speed = s
        setPref("speed", speed.toString())
        setSpeedButton(s)
        hideAllMenus()
    }

    fun speedToIncrement(s:Speed): Int {
        return when(s) {
            Speed.SLOW -> 1
            Speed.MEDIUM -> 2
            Speed.FAST -> 5
        }
    }

    fun speedToBlendAlpha(s: Speed): Int {
        return when(s) {
            Speed.SLOW -> 10
            Speed.MEDIUM -> 20
            Speed.FAST -> 50
        }
    }

    fun setPref(key: String, value: String) {
        getPreferences(MODE_PRIVATE).edit().let { editPrefs ->
            editPrefs.putString(key, value)
            editPrefs.commit()
        }
    }

    fun setModeButton(m: Mode) {
        modeButton.setImageResource(modeIcon(m))
        (modeButton.drawable as AnimationDrawable?)?.let { anim -> anim.start() }
    }

    fun setSpeedButton(s: Speed) {
        speedButton.setImageResource(speedIcon(s))
    }
}