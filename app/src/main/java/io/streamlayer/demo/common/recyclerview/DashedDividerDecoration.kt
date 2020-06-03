package io.streamlayer.demo.common.recyclerview

import android.content.Context
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import io.streamlayer.demo.R

class DashedDividerDecoration(context: Context) : ItemDecoration() {
    private val dividerSize: Int by lazy {
        context.resources.getDimensionPixelSize(R.dimen.divider_size)
    }
    private val dividerDashWidth: Float by lazy {
        context.resources.getDimensionPixelSize(R.dimen.divider_dash_width).toFloat()
    }
    private val dividerDashSpacing: Float by lazy {
        context.resources.getDimensionPixelSize(R.dimen.divider_dash_spacing).toFloat()
    }

    private val paint: Paint by lazy {
        Paint().apply {
            color = ContextCompat.getColor(
                context,
                R.color.divider
            )
            style = Paint.Style.STROKE
            strokeWidth = dividerSize.toFloat()
            pathEffect = DashPathEffect(floatArrayOf(dividerDashWidth, dividerDashSpacing), 0.0f)
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = dividerSize
    }

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val left = parent.paddingLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount
        val path = Path()
        if (childCount > 1) {
            for (i in 0 until childCount - 1) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as ViewGroup.MarginLayoutParams
                val top = child.bottom + params.bottomMargin + dividerSize / 2
                path.moveTo(left.toFloat(), top.toFloat())
                path.lineTo(right.toFloat(), top.toFloat())
            }
        }
        c.drawPath(path, paint)
    }

}