package io.streamlayer.demo.common.ext

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.view.*
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

private const val KEYBOARD_MIN_HEIGHT_RATIO = 0.15f

fun Context.isScreenPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

val FragmentActivity.windowController: WindowInsetsControllerCompat
    get() = WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

fun Window.changeFullScreen(
    windowController: WindowInsetsControllerCompat,
    isEnter: Boolean
) {
    WindowCompat.setDecorFitsSystemWindows(this, !isEnter)
    if (isEnter) windowController.hide(WindowInsetsCompat.Type.systemBars())
    else windowController.show(WindowInsetsCompat.Type.systemBars())
}

fun Window.keepOnScreen() {
    addFlags(
        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
    )
}

@SuppressLint("NewApi")
fun FragmentActivity.isMultiWindowOrPiPModeEnabled(): Boolean = kotlin.runCatching {
    val supportFeatures = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    val isMultiWindow = if (supportFeatures) isInMultiWindowMode else false
    val isPictureInPicture = if (supportFeatures) isInPictureInPictureMode else false
    isMultiWindow || isPictureInPicture
}.getOrDefault(false)

fun FragmentActivity.isKeyboardVisible(): Boolean {
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

fun FragmentActivity.getActivityRoot(): View {
    return getContentRoot().rootView
}

fun FragmentActivity.getContentRoot(): ViewGroup {
    return findViewById(android.R.id.content)
}

fun FragmentActivity.setInputKeyboardEventListener(listener: (Boolean) -> Unit) {
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