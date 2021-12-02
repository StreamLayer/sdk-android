package io.streamlayer.demo.main.brand

import android.os.Bundle
import android.view.View
import io.streamlayer.demo.R
import io.streamlayer.demo.common.koin.injectViewModel
import io.streamlayer.demo.common.kotlin.bindingDelegate
import io.streamlayer.demo.common.kotlin.visibleIf
import io.streamlayer.demo.common.mvvm.BaseFragment
import io.streamlayer.demo.common.mvvm.collectWhenStarted
import io.streamlayer.demo.databinding.FragmentBrandBinding
import io.streamlayer.demo.player.PlayerActivity

class BrandFragment : BaseFragment(R.layout.fragment_brand) {

    private val viewModel: BrandViewModel by injectViewModel()

    private val binding by bindingDelegate(FragmentBrandBinding::bind)

    private val brandAdapter: BrandAdapter by lazy { BrandAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        bind()
    }

    private fun setupUI() {
        with(binding) {
            viewPager.apply {
                isUserInputEnabled = false
                adapter = brandAdapter
                brandAdapter.onItemSelected = { PlayerActivity.open(requireContext(), it.eventId.toString()) }
            }
            streamButton.setOnClickListener { viewModel.selectType(Type.STREAM) }
            articlesButton.setOnClickListener { viewModel.selectType(Type.ARTICLES) }
        }
    }

    private fun bind() {
        with(binding) {
            viewModel.brands.collectWhenStarted(viewLifecycleOwner) {
                viewPager.visibleIf(it.isNotEmpty())
                dataLoader.visibleIf(it.isEmpty())
                brandAdapter.setItems(it)
            }
            viewModel.type.collectWhenStarted(viewLifecycleOwner) {
                val isStreamSelected = it == Type.STREAM
                streamButton.setBackgroundResource(if (isStreamSelected) R.drawable.brand_type_selected_bg else R.drawable.brand_type_unselected_bg)
                articlesButton.setBackgroundResource(if (!isStreamSelected) R.drawable.brand_type_selected_bg else R.drawable.brand_type_unselected_bg)
                viewPager.setCurrentItem(if (isStreamSelected) 0 else 1, false)
            }
        }
    }
}