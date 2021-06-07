package io.streamlayer.demo.common.kotlin

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

internal fun ImageView.loadUrl(
    url: String,
    crossFade: Boolean = true,
    errorResId: Int? = null,
    onLoadingFinished: () -> Unit = {}
) {
    val listener = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }

    }
    val requestOptions = RequestOptions().dontTransform()
    Glide.with(this)
        .load(url)
        .apply {
            // keep in mind, CircularImageView library doesn't work with CIV for some reason
            if (crossFade) {
                transition(DrawableTransitionOptions().crossFade(200))
            }
            errorResId?.let { error(it) }
        }
        .apply(requestOptions)
        .listener(listener)
        .into(this)
}

internal fun ImageView.setDrawable(@DrawableRes resId: Int) {
    setImageDrawable(resources.getDrawable(resId))
    visible()
}

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

internal fun View.visibleIfElse(condition: Boolean, elseValue: Int = INVISIBLE) {
    visibility = if (condition) VISIBLE else elseValue
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