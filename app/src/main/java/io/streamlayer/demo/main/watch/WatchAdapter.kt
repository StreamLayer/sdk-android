package io.streamlayer.demo.main.watch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.streamlayer.demo.common.kotlin.dp
import io.streamlayer.demo.databinding.ItemWatchBinding
import io.streamlayer.demo.main.HorizontalOffsetDecoration
import io.streamlayer.demo.main.StreamsAdapter
import io.streamlayer.demo.repository.Stream

class WatchAdapter : RecyclerView.Adapter<WatchAdapter.ViewHolder>() {

    private val items: MutableList<Watch> = mutableListOf()

    var onItemSelected: ((Stream) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemWatchBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(listItems: List<Watch>) {
        this.items.clear()
        this.items.addAll(listItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(binding: ItemWatchBinding) : RecyclerView.ViewHolder(binding.root) {

        private val mainAdapter: StreamsAdapter by lazy { StreamsAdapter(Pair(299f.dp, 158f.dp)) }

        private val moreAdapter: StreamsAdapter by lazy { StreamsAdapter(Pair(231f.dp, 112f.dp)) }

        private val recommendedAdapter: StreamsAdapter by lazy { StreamsAdapter(Pair(182f.dp, 84f.dp)) }

        init {
            with(binding) {
                mainRecycler.apply {
                    addItemDecoration(HorizontalOffsetDecoration(15f.dp))
                    adapter = mainAdapter
                    mainAdapter.onItemSelected = { onItemSelected?.invoke(it) }
                }
                moreRecycler.apply {
                    addItemDecoration(HorizontalOffsetDecoration(15f.dp))
                    adapter = moreAdapter
                    moreAdapter.onItemSelected = { onItemSelected?.invoke(it) }
                }
                recommendedRecycler.apply {
                    addItemDecoration(HorizontalOffsetDecoration(15f.dp))
                    adapter = recommendedAdapter
                    recommendedAdapter.onItemSelected = { onItemSelected?.invoke(it) }
                }
            }
        }

        fun bind(data: Watch) {
            mainAdapter.setItems(data.main)
            moreAdapter.setItems(data.more)
            recommendedAdapter.setItems(data.recommended)
        }
    }
}