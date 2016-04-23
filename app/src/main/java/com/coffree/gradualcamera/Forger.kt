package com.coffree.gradualcamera

import android.graphics.*
import android.util.Log

/**
 * Created by fraser on 11/04/16.
 */
abstract class Forger(val target: Bitmap?) {

    val targetCanvas = Canvas(target)

    /* takes a new frame and composites it into the current bitmap somehow
        returns true if we are done, otherwise false
     */
    abstract fun update(frame: Bitmap): Boolean
    abstract fun terminates(): Boolean
}
