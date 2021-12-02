package io.streamlayer.demo.main.brand

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.streamlayer.demo.common.kotlin.dp
import io.streamlayer.demo.databinding.ItemBrandBinding
import io.streamlayer.demo.main.HorizontalOffsetDecoration
import io.streamlayer.demo.main.StreamsAdapter
import io.streamlayer.demo.repository.Stream

class BrandAdapter : RecyclerView.Adapter<BrandAdapter.ViewHolder>() {

    private val items: MutableList<Brand> = mutableListOf()

    var onItemSelected: ((Stream) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemBrandBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(listItems: List<Brand>) {
        this.items.clear()
        this.items.addAll(listItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(binding: ItemBrandBinding) : RecyclerView.ViewHolder(binding.root) {

        private val nowAdapter: StreamsAdapter by lazy { StreamsAdapter(Pair(231f.dp, 112f.dp)) }

        private val recommendedAdapter: StreamsAdapter by lazy { StreamsAdapter(Pair(182f.dp, 84f.dp)) }

        init {
            with(binding) {
                nowRecycler.apply {
                    addItemDecoration(HorizontalOffsetDecoration(15f.dp))
                    adapter = nowAdapter
                    nowAdapter.onItemSelected = { onItemSelected?.invoke(it) }
                }
                recommendedRecycler.apply {
                    addItemDecoration(HorizontalOffsetDecoration(15f.dp))
                    adapter = recommendedAdapter
                    recommendedAdapter.onItemSelected = { onItemSelected?.invoke(it) }
                }
            }
        }

        fun bind(data: Brand) {
            nowAdapter.setItems(data.now)
            recommendedAdapter.setItems(data.recommended)
        }
    }
}