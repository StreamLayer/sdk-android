package io.streamlayer.demo.managed.api

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.streamlayer.demo.R
import io.streamlayer.demo.common.ext.BaseFragment
import io.streamlayer.demo.common.ext.bindingDelegate
import io.streamlayer.demo.common.ext.collectWhenStarted
import io.streamlayer.demo.common.ext.visibleIf
import io.streamlayer.demo.databinding.FragmentManagedGroupMessagesBinding
import io.streamlayer.demo.databinding.ItemManagedGroupMessageBinding

class ManagedGroupMessagesFragment : BaseFragment(R.layout.fragment_managed_group_messages) {

    private val viewModel: ManagedGroupViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private val binding by bindingDelegate(FragmentManagedGroupMessagesBinding::bind)

    private val messagesAdapter: MessagesAdapter by lazy { MessagesAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        bind()
    }

    private fun setupUI() {
        with(binding) {
            val chatLayoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, true)
            messagesList.layoutManager = chatLayoutManager
            messagesList.adapter = messagesAdapter
            messagesAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (positionStart == 0 && chatLayoutManager.findFirstVisibleItemPosition() < 2) {
                        chatLayoutManager.scrollToPosition(0)
                    }
                }
            })
            messageSendFAB.setOnClickListener {
                val message = messageInput.text.toString()
                if (message.isEmpty()) return@setOnClickListener
                messageInput.text = null
                viewModel.sendMessage(message)
            }
        }
    }

    private fun bind() {
        with(binding) {
            viewModel.isActive.collectWhenStarted(viewLifecycleOwner) {
                emptyView.visibleIf(!it)
                messagesList.visibleIf(it)
                messageInput.visibleIf(it)
                messageSendFAB.visibleIf(it)
            }
            viewModel.messages.collectWhenStarted(viewLifecycleOwner) {
                messagesAdapter.setItems(it)
            }
        }
    }
}

private class MessagesAdapter : RecyclerView.Adapter<MessagesAdapter.ViewHolder>() {

    private val items = mutableListOf<Message>()

    override fun getItemCount(): Int = items.count()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemManagedGroupMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setItems(items: List<Message>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemManagedGroupMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.chatMessageContainer.updateLayoutParams<ConstraintLayout.LayoutParams> {
                this.horizontalBias = if (message.isLocal) 0f else 1f
            }
            binding.chatMessageContainer.setBackgroundResource(
                if (message.isLocal) R.drawable.slr_shape_chat_bubble_contact else R.drawable.slr_shape_chat_bubble_user
            )
            binding.userView.text = "${message.userId}"
            binding.messageContent.text = message.content
        }
    }
}