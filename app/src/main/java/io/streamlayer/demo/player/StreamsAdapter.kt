package io.streamlayer.demo.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import io.streamlayer.demo.R
import io.streamlayer.demo.common.kotlin.dp
import io.streamlayer.demo.common.kotlin.gone
import io.streamlayer.demo.common.kotlin.visible
import io.streamlayer.demo.common.kotlin.visibleIf
import io.streamlayer.demo.databinding.ItemStreamVerticalBinding
import io.streamlayer.demo.repository.Stream

class StreamsAdapter : RecyclerView.Adapter<StreamsAdapter.ViewHolder>() {

    private val items: MutableList<Stream> = mutableListOf()

    var onItemSelected: ((Stream) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemStreamVerticalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(listItems: List<Stream>) {
        this.items.clear()
        this.items.addAll(listItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemStreamVerticalBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: Stream) {
            with(binding) {
                thumbnail.load(data.eventImageUrl) {
                    error(R.drawable.nbc_placeholder)
                    fallback(R.drawable.nbc_placeholder)
                    transformations(RoundedCornersTransformation(8f.dp.toFloat()))
                }
                title.text = data.title
                if (data.subtitle.isNotEmpty()) {
                    description.text = data.subtitle
                    description.visible()
                } else description.gone()
                liveIndicator.visibleIf(data.live)
                time.text = data.time
                root.setOnClickListener { onItemSelected?.invoke(data) }
            }
        }
    }
}

class DashedDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
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