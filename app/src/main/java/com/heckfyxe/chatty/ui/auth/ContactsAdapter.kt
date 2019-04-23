package com.heckfyxe.chatty.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Contact
import kotlinx.android.synthetic.main.item_contact.view.*

class ContactsAdapter : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private val contactList = mutableListOf<Contact>()

    class ContactDiff(private val oldList: List<Contact>, private val newList: List<Contact>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].number == newList[newItemPosition].number

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    fun update(list: List<Contact>) {
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
        holder.bind(contactList[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(contact: Contact) {
            itemView.apply {
                contactNumber.text = contact.number
                contactName.text = contact.name
            }
        }
    }
}