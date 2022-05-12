package io.streamlayer.demo.common.ext

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

private const val KEYBOARD_MIN_HEIGHT_RATIO = 0.15f

/**
 * Determine if keyboard is visible
 *
 * @return Whether keyboard is visible or not
 */
internal fun FragmentActivity.isKeyboardVisible(): Boolean {
    val r = Rect()
    val activityRoot = getActivityRoot()
    activityRoot.getWindowVisibleDisplayFrame(r)
    val location = IntArray(2)
    getContentRoot().getLocationOnScreen(location)
    val isScreenPortrait = isScreenPortrait()
    val screenHeight = activityRoot.rootView.height
    val screenWidth = activityRoot.rootView.width
    if ((isScreenPortrait && screenWidth > screenHeight) || (!isScreenPortrait && screenHeight > screenWidth))
        return false // skip calculation during configuration changes
    val heightDiff = screenHeight - r.height() - location[1]
    return heightDiff > screenHeight * KEYBOARD_MIN_HEIGHT_RATIO
}

internal fun FragmentActivity.getActivityRoot(): View {
    return getContentRoot().rootView
}

internal fun FragmentActivity.getContentRoot(): ViewGroup {
    return findViewById(android.R.id.content)
}

internal fun FragmentActivity.setInputKeyboardEventListener(listener: (Boolean) -> Unit) {
    val softInputAdjust =
        this.window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST

    val isNotAdjustNothing =
        softInputAdjust and WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
    require(isNotAdjustNothing) { "Parameter:activity window SoftInputMethod is SOFT_INPUT_ADJUST_NOTHING. In this case window will not be resized" }

    val activityRoot = getActivityRoot()
    val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        private var wasOpened = false

        override fun onGlobalLayout() {
            val isOpen = isKeyboardVisible()
            if (isOpen == wasOpened) {
                // keyboard state has not changed
                return
            }
            wasOpened = isOpen
            listener.invoke(isOpen)
        }
    }
    activityRoot.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    lifecycle.addObserver(object : DefaultLifecycleObserver {

        override fun onDestroy(owner: LifecycleOwner) {
            lifecycle.removeObserver(this)
            val activityRoot = getActivityRoot()
            @Suppress("DEPRECATION")
            activityRoot.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
    })
}