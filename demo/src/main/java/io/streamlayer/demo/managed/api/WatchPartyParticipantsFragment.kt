package io.streamlayer.demo.managed.api

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.*
import io.streamlayer.demo.common.ext.bindingDelegate
import io.streamlayer.demo.common.ext.visibleIf
import io.streamlayer.demo.databinding.FragmentWatchPartyParticipantsBinding
import io.streamlayer.demo.databinding.ItemWatchPartyParticipantBinding

class WatchPartyParticipantsFragment : BaseFragment(R.layout.fragment_watch_party_participants) {

    private val viewModel: WatchPartyViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private val binding by bindingDelegate(FragmentWatchPartyParticipantsBinding::bind)

    private val participantsAdapter: ParticipantsAdapter by lazy { ParticipantsAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        bind()
    }

    private fun setupUI() {
        with(binding) {
            slrParticipantsView.apply {
                addItemDecoration(DashedDividerDecoration(requireContext()))
                adapter = participantsAdapter
            }
            slSubscribeButton.setOnClickListener {
                val groupId = slGroupIdView.text?.toString()?.trim()
                if (groupId.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Group id can not be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                viewModel.subscribe(groupId)
            }
            slUnsubscribeButton.setOnClickListener { viewModel.unsubscribe() }
            slrOpenWatchPartyBtn.setOnClickListener { viewModel.openWatchParty() }
        }
    }

    private fun bind() {
        viewModel.isActive.collectWhenStarted(viewLifecycleOwner) {
            with(binding) {
                slrEmptyView.visibleIf(!it)
                slrParticipantsView.visibleIf(it)
                slrOpenWatchPartyBtn.visibleIf(it)
            }
        }
        viewModel.participants.collectWhenStarted(viewLifecycleOwner) {
            Log.d("WatchPartySession", "participants $it")
            participantsAdapter.setItems(it)
        }
        viewModel.viewEvents.collectWhenResumed(viewLifecycleOwner) {
            when (it) {
                is ShowWatchParty -> {
                    view.hideKeyboard()
                    it.watchPartySession.show(requireActivity())
                }
                is BaseErrorEvent -> Toast.makeText(requireContext(), it.error, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

private class ParticipantsAdapter : RecyclerView.Adapter<ParticipantsAdapter.ViewHolder>() {

    private val items = mutableListOf<Participant>()

    override fun getItemCount(): Int = items.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemWatchPartyParticipantBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setItems(items: List<Participant>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemWatchPartyParticipantBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(participant: Participant) {
            binding.participantId.text = "${participant.user.bypassId}\n${participant.user.id}"
            binding.statusView.text = "${participant.status}"
        }
    }
}

private class DashedDividerDecoration(context: Context) : RecyclerView.ItemDecoration() {
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