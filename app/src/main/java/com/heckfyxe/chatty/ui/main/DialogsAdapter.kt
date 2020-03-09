package com.heckfyxe.chatty.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.databinding.ItemDialogBinding
import com.heckfyxe.chatty.model.Dialog

typealias DialogClickListener = (Dialog) -> Unit

class DialogsAdapter(private val clickListener: DialogClickListener) :
    ListAdapter<Dialog, DialogsAdapter.ViewHolder>(DIFF) {

    private companion object DIFF : DiffUtil.ItemCallback<Dialog>() {
        override fun areItemsTheSame(oldItem: Dialog, newItem: Dialog): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Dialog, newItem: Dialog): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemDialogBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class ViewHolder(private val binding: ItemDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(dialog: Dialog, clickListener: DialogClickListener) {
            binding.dialog = dialog
            binding.executePendingBindings()
            itemView.setOnClickListener {
                clickListener.invoke(dialog)
            }
        }
    }
}