package io.streamlayer.demo.player

import android.view.View
import com.bornfight.utils.adapters.GenericAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.streamlayer.demo.R
import io.streamlayer.demo.utils.dp
import io.streamlayer.demo.utils.gone
import io.streamlayer.demo.utils.visible
import io.streamlayer.demo.utils.visibleIf
import io.streamlayer.sdk.StreamLayerDemo
import kotlinx.android.synthetic.main.item_stream.view.*

class StreamsAdapter : GenericAdapter<StreamLayerDemo.Item>() {
    var onItemSelected: ((StreamLayerDemo.Item) -> Unit)? = null

    override fun getLayoutId(viewType: Int): Int =
        R.layout.item_stream
    override fun getViewHolder(view: View, viewType: Int): GenericViewHolder<StreamLayerDemo.Item> {
        return StreamViewHolder(view)
    }

    override fun getItemId(position: Int): Long = position.toLong()

    inner class StreamViewHolder(itemView: View) :
        GenericViewHolder<StreamLayerDemo.Item>(itemView) {
        override fun bind(data: StreamLayerDemo.Item) {
            with(itemView) {
                Glide.with(this)
                    .load(data.eventImageUrl)
                    .error(R.drawable.nbc_placeholder)
                    .fallback(R.drawable.nbc_placeholder)
                    .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(8f.dp)))
                    .into(thumbnail)

                title.text = data.title

                if (data.subtitle.isNotEmpty()) {
                    description.text = data.subtitle
                    description.visible()
                } else {
                    description.gone()
                }

                liveIndicator.visibleIf(data.live)
                time.text = data.time

                setOnClickListener {
                    onItemSelected?.invoke(data)
                }
            }
        }
    }
}