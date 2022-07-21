package io.streamlayer.demo.common.ext

import android.os.Handler
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import kotlin.math.abs
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

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