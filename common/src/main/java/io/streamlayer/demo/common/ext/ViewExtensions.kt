package io.streamlayer.demo.common.ext

import android.content.res.Resources
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

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

