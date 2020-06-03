package io.streamlayer.demo.common.kotlin

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.ImageView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target

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

internal fun ImageView.setDrawable(resId: Int) {
    setImageDrawable(VectorDrawableCompat.create(resources, resId, null))
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
