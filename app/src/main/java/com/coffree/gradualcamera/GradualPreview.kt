package com.coffree.gradualcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.Camera
import android.renderscript.*
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Created by fraser on 09/04/16.
 */
class GradualPreview(context: Context, var camera: Camera) : SurfaceView(context), SurfaceHolder.Callback, Camera.PreviewCallback {
    init {
        holder.addCallback(this)
    }

    val TAG = "GradualPreview"
    var frameBuffer: ByteArray? = null
    var frameBitmap: Bitmap? = null
    var startTime: Long = 0
    var frameCount: Long = 0

    var renderScript: RenderScript? = null
    var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    //var yuvType: Type.Builder? = null
    var allocIn: Allocation? = null
    //var rgbaType: Type.Builder? = null
    var allocOut: Allocation? = null

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

    override fun onPreviewFrame(nv21: ByteArray, camera: Camera?) {
        val bm = frameBitmap
        if (bm != null && nv21 != null) {
            //Log.d(TAG, "raw frame length ${nv21.size}")
            val before = System.currentTimeMillis()
            allocIn?.copyFrom(nv21)
            yuvToRgbIntrinsic?.setInput(allocIn)
            yuvToRgbIntrinsic?.forEach(allocOut)
            allocOut?.copyTo(bm)
            ++frameCount
            Log.d(TAG, "this frame ${System.currentTimeMillis() - before} milliseconds, ${1000 * frameCount / (System.currentTimeMillis() - startTime)} preview frames/second")
            camera?.addCallbackBuffer(frameBuffer)
        }
    }

    private fun startPreview() {
        if (holder!!.surface != null) {
            try {
                val params = camera.parameters
                val dimensions = params.previewSize
                val format = params.previewFormat
                frameBuffer = ByteArray(dimensions.width * dimensions.height * ImageFormat.getBitsPerPixel(format))
                val buf = frameBuffer
                if (buf != null) {
                    frameBitmap = Bitmap.createBitmap(dimensions.width, dimensions.height, Bitmap.Config.ARGB_8888)
                    renderScript = RenderScript.create(context)
                    yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(renderScript, Element.U8_4(renderScript))
                    val yuvType = Type.Builder(renderScript, Element.U8(renderScript)).setX(buf.size)
                    allocIn = Allocation.createTyped(renderScript, yuvType.create(), Allocation.USAGE_SCRIPT)
                    val rgbaType = Type.Builder(renderScript, Element.RGBA_8888(renderScript)).setX(dimensions.width).setY(dimensions.height)
                    allocOut = Allocation.createTyped(renderScript, rgbaType.create(), Allocation.USAGE_SCRIPT)
                    camera.addCallbackBuffer(frameBuffer)
                    camera.setPreviewDisplay(holder)
                    //camera.setPreviewCallbackWithBuffer(this)
                    camera.startPreview()
                }
                startTime = System.currentTimeMillis()
                frameCount = 0
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: " + e.message);
            }
        }
    }

    private fun stopPreview() {
        try {
            camera.setPreviewCallback(null)
            camera.stopPreview()
            frameBuffer = null
            frameBitmap = null
        } catch (e: Exception) {
            // do nothing, no preview exists
        }
    }
}