package io.streamlayer.demo.common.ext

import android.content.res.Resources
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import kotlin.math.abs

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

infix fun View.visibleIf(boolean: Boolean) {
    visibility = if (boolean) View.VISIBLE else View.GONE
}

val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun View?.hideKeyboard() {
    this?.let {
        context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

fun View?.showKeyboard() {
    this?.let {
        if (it.requestFocus()) {
            context.getSystemService<InputMethodManager>()?.showSoftInput(it, 0)
        }
    }
}

private const val TAP_DELAY = 300L
private const val PRESS_TOUCH_SNAP = 30

// double tap listener
abstract class DoubleTapListener : View.OnTouchListener {
    // detecting move and tap events
    private val handler = Handler()
    private var initialTapX = 0f
    private var initialTapY = 0f
    private var doubleTapX = 0f
    private var doubleTapY = 0f
    private var doubleTapMillis = 0L
    private val onTap = Runnable { onDelayedTap(doubleTapX, doubleTapY) }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialTapX = event.x
                initialTapY = event.y
                // check if double tap is still active
                if (doubleTapMillis > 0L) {
                    if (System.currentTimeMillis() - doubleTapMillis < TAP_DELAY) {
                        if (abs(doubleTapX - event.x) >= PRESS_TOUCH_SNAP || abs(doubleTapY - event.y) >= PRESS_TOUCH_SNAP) {
                            cancelDoubleTap()
                            onDelayedTap(initialTapX, initialTapY)
                        } else {
                            // consume double tap event
                            cancelDoubleTap()
                            onDoubleTap(initialTapX, initialTapY)
                        }
                    } else startDoubleTap(event.x, event.y)
                } else startDoubleTap(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                // check if double tap still actual
                if (abs(doubleTapX - event.x) >= PRESS_TOUCH_SNAP || abs(doubleTapY - event.y) >= PRESS_TOUCH_SNAP) cancelDoubleTap()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // check if double tap is still active
                if (doubleTapMillis > 0L && System.currentTimeMillis() - doubleTapMillis > TAP_DELAY) cancelDoubleTap()
                initialTapX = 0f
                initialTapY = 0f
            }
        }
        return true
    }

    private fun cancelDoubleTap() {
        handler.removeCallbacks(onTap)
        doubleTapMillis = 0L
        doubleTapX = 0f
        doubleTapY = 0f
    }

    private fun startDoubleTap(x: Float, y: Float) {
        doubleTapMillis = System.currentTimeMillis()
        doubleTapX = x
        doubleTapY = y
        handler.postDelayed(onTap, TAP_DELAY)
    }

    abstract fun onDelayedTap(x: Float, y: Float)

    abstract fun onDoubleTap(x: Float, y: Float)
}

