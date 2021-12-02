package io.streamlayer.demo.main.watch

import android.os.Bundle
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import io.streamlayer.demo.R
import io.streamlayer.demo.common.koin.injectViewModel
import io.streamlayer.demo.common.kotlin.bindingDelegate
import io.streamlayer.demo.common.kotlin.visibleIf
import io.streamlayer.demo.common.mvvm.BaseFragment
import io.streamlayer.demo.common.mvvm.collectWhenStarted
import io.streamlayer.demo.databinding.FragmentWatchBinding
import io.streamlayer.demo.player.PlayerActivity

class WatchFragment : BaseFragment(R.layout.fragment_watch) {

    private val viewModel: WatchViewModel by injectViewModel()

    private val binding by bindingDelegate(FragmentWatchBinding::bind)

    private val watchAdapter: WatchAdapter by lazy { WatchAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        bind()
    }

    private fun setupUI() {
        with(binding) {
            viewPager.apply {
                isUserInputEnabled = false
                adapter = watchAdapter
                watchAdapter.onItemSelected = { PlayerActivity.open(requireContext(),it.eventId.toString())}
                TabLayoutMediator(tabLayout, this, true, false) { tab, position ->
                    tab.text = when (position) {
                        0 -> getString(R.string.featured)
                        else -> getString(R.string.originals)
                    }
                }.attach()
            }
        }
    }

    private fun bind() {
        with(binding) {
            viewModel.watches.collectWhenStarted(viewLifecycleOwner) {
                viewPager.visibleIf(it.isNotEmpty())
                dataLoader.visibleIf(it.isEmpty())
                watchAdapter.setItems(it)
            }
            viewModel.type.collectWhenStarted(viewLifecycleOwner) {
                val isFeaturedSelected = it == Type.FEATURED
                viewPager.setCurrentItem(if (isFeaturedSelected) 0 else 1, false)
            }
        }
    }
}