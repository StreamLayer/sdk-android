package io.streamlayer.demo.utils

import android.content.res.Resources
import android.os.Handler
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes

private const val DOUBLE_CLICK_TIME_DELTA = 300L

class DoubleClickListener(private val onSingleTap: () -> Unit, private val onDoubleTap: () -> Unit) :
    View.OnClickListener {

    private val handler = Handler()
    private val runnable = Runnable { onSingleTap() }
    private var lastClickTime = 0L

    override fun onClick(v: View?) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            handler.removeCallbacks(runnable)
            lastClickTime = 0
            onDoubleTap()
        } else {
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, DOUBLE_CLICK_TIME_DELTA)
            lastClickTime = clickTime
        }
    }
}

internal fun ImageView.setDrawable(@DrawableRes resId: Int) {
    setImageDrawable(resources.getDrawable(resId))
    visible()
}

internal fun View.visible() {
    visibility = View.VISIBLE
}

internal fun View.invisible() {
    visibility = View.INVISIBLE
}

internal fun View.gone() {
    visibility = View.GONE
}

internal infix fun View.visibleIf(boolean: Boolean) {
    visibility = if (boolean) View.VISIBLE else View.GONE
}

internal val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()