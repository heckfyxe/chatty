package com.heckfyxe.chatty.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.ContactWithId
import kotlinx.android.synthetic.main.item_contact.view.*

class ContactsAdapter(private val contactCheckAction: (String, Boolean) -> Unit) :
    RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private val contactList = mutableListOf<ContactWithId>()

    class ContactDiff(
        private val oldList: List<ContactWithId>,
        private val newList: List<ContactWithId>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].uid == newList[newItemPosition].uid

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    fun update(list: List<ContactWithId>) {
        val diff = DiffUtil.calculateDiff(ContactDiff(contactList, list))
        contactList.clear()
        contactList.addAll(list)
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = contactList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(contactList[position], contactCheckAction)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(contactWithId: ContactWithId, checkAction: (String, Boolean) -> Unit) {
            val contact = contactWithId.contact
            itemView.apply {
                contactNumber.text = contact.number
                contactName.text = contact.name
                tag = contactWithId.uid
                contactCheckBox?.setOnCheckedChangeListener { _, isChecked ->
                    checkAction(contactWithId.uid, isChecked)
                }
            }
        }
    }
}