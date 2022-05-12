package io.streamlayer.demo.common.ext

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import kotlin.math.abs
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun View.visible() {
    visibility = VISIBLE
}

internal fun View.invisible() {
    visibility = INVISIBLE
}

internal fun View.gone() {
    visibility = GONE
}

internal infix fun View.visibleIf(boolean: Boolean) {
    visibility = if (boolean) VISIBLE else GONE
}

internal val Float.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

// you need check that view is not null only for onSaveInstanceState() before using this delegate method
// https://gist.github.com/gmk57/aefa53e9736d4d4fb2284596fb62710d#gistcomment-3604657
internal fun <T : ViewBinding> Fragment.bindingDelegate(viewBindingFactory: (View) -> T) =
    FragmentViewBindingDelegate(this, viewBindingFactory)

internal class FragmentViewBindingDelegate<T : ViewBinding>(
    val fragment: Fragment,
    val viewBindingFactory: (View) -> T
) : ReadOnlyProperty<Fragment, T> {
    private var binding: T? = null

    init {
        fragment.lifecycle.addObserver(object : DefaultLifecycleObserver {
            val viewLifecycleOwnerLiveDataObserver =
                Observer<LifecycleOwner?> {
                    val viewLifecycleOwner = it ?: return@Observer

                    viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                        override fun onDestroy(owner: LifecycleOwner) {
                            binding = null
                        }
                    })
                }

            override fun onCreate(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.observeForever(viewLifecycleOwnerLiveDataObserver)
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragment.viewLifecycleOwnerLiveData.removeObserver(viewLifecycleOwnerLiveDataObserver)
            }
        })
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val binding = binding
        if (binding != null) return binding

        val lifecycle = fragment.viewLifecycleOwner.lifecycle
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
            throw IllegalStateException("Should not attempt to get bindings when Fragment views are destroyed.")
        }

        return viewBindingFactory(thisRef.requireView()).also { this.binding = it }
    }
}

private const val TAP_DELAY = 300L
private const val PRESS_TOUCH_SNAP = 30

// double tap listener
internal abstract class DoubleTapListener : View.OnTouchListener {
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


internal fun Context.isScreenPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

/**
 * Hides the on screen keyboard.
 */
internal fun View?.hideKeyboard() {
    this?.let {
        context.getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(it.windowToken, 0)
    }
}

/**
 * Shows the on screen keyboard.
 */
internal fun View?.showKeyboard() {
    this?.let {
        if (it.requestFocus()) {
            context.getSystemService<InputMethodManager>()?.showSoftInput(it, 0)
        }
    }
}


internal val FragmentActivity.windowController: WindowInsetsControllerCompat
    get() = WindowInsetsControllerCompat(window, window.decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

internal fun Window.changeFullScreen(windowController: WindowInsetsControllerCompat, isEnter: Boolean) {
    WindowCompat.setDecorFitsSystemWindows(this, !isEnter)
    if (isEnter) windowController.hide(WindowInsetsCompat.Type.systemBars())
    else windowController.show(WindowInsetsCompat.Type.systemBars())
}

@SuppressLint("NewApi")
internal fun FragmentActivity.isMultiWindowOrPiPModeEnabled(): Boolean = kotlin.runCatching {
    val supportFeatures = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    val isMultiWindow = if (supportFeatures) isInMultiWindowMode else false
    val isPictureInPicture = if (supportFeatures) isInPictureInPictureMode else false
    isMultiWindow || isPictureInPicture
}.getOrDefault(false)

