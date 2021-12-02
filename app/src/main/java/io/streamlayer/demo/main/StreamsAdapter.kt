package io.streamlayer.demo.main

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import io.streamlayer.demo.R
import io.streamlayer.demo.common.kotlin.dp
import io.streamlayer.demo.common.kotlin.invisible
import io.streamlayer.demo.common.kotlin.visible
import io.streamlayer.demo.databinding.ItemStreamHorizontalBinding
import io.streamlayer.demo.repository.Stream

class StreamsAdapter(private val imageSize: Pair<Int, Int>) : RecyclerView.Adapter<StreamsAdapter.ViewHolder>() {

    private val items: MutableList<Stream> = mutableListOf()

    var onItemSelected: ((Stream) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemStreamHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(listItems: List<Stream>) {
        this.items.clear()
        this.items.addAll(listItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemStreamHorizontalBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.updateLayoutParams {
                width = imageSize.first
            }
            binding.thumbnail.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = imageSize.first
                height = imageSize.second
            }
        }

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
                } else description.invisible()
                root.setOnClickListener { onItemSelected?.invoke(data) }
            }
        }
    }
}

class HorizontalOffsetDecoration(private val offset: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val itemPosition = parent.getChildAdapterPosition(view)
        val lastPosition = (parent.adapter?.itemCount ?: 0) - 1
        if (lastPosition > 0 && lastPosition == itemPosition) outRect.right = 0 else outRect.right = offset
    }
}