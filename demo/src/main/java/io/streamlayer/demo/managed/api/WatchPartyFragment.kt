package io.streamlayer.demo.managed.api

import android.os.Bundle
import android.util.SparseArray
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.BaseFragment
import io.streamlayer.demo.common.ext.bindingDelegate
import io.streamlayer.demo.common.ext.collectWhenStarted
import io.streamlayer.demo.databinding.FragmentWatchPartyBinding
import kotlin.reflect.KClass

class WatchPartyFragment : BaseFragment(R.layout.fragment_watch_party) {

    private val viewModel: WatchPartyViewModel by viewModels()

    private val binding by bindingDelegate(FragmentWatchPartyBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        bind()
    }

    private fun setupUI() {
        val adapter = Adapter(
            this, listOf(
                getString(R.string.participants) to WatchPartyParticipantsFragment::class,
                getString(R.string.messages) to WatchPartyMessagesFragment::class,
            )
        )
        with(binding) {
            viewPager.adapter = adapter
            TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
                tab.text = adapter.getPageTitle(pos)
            }.attach()
        }
    }

    private fun bind() {
        viewModel.isLoading.collectWhenStarted(viewLifecycleOwner) {
            showLoader(it)
        }
    }
}

private class Adapter(
    fm: Fragment,
    private val items: List<Pair<String, KClass<out Fragment>>>
) : FragmentStateAdapter(fm) {

    private val fragmentInstances = SparseArray<Fragment?>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun createFragment(position: Int): Fragment {
        var instance = fragmentInstances.get(position)
        if (instance == null) {
            instance = items[position].second.java.newInstance()
            fragmentInstances[position] = instance
        }
        return items[position].second.java.newInstance()
    }

    fun getPageTitle(position: Int): CharSequence? {
        return items[position].first
    }
}