package com.heckfyxe.chatty.ui.auth

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.CheckingContact
import kotlinx.android.synthetic.main.item_contact.view.*

class ContactsAdapter(private val contactCheckAction: (CheckingContact) -> Unit) :
    RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private val contactList = mutableListOf<CheckingContact>()

    class ContactDiff(
        private val oldList: List<CheckingContact>,
        private val newList: List<CheckingContact>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].contactWithId.uid == newList[newItemPosition].contactWithId.uid

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    fun update(list: List<CheckingContact>) {
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
        fun bind(checkingContact: CheckingContact, checkAction: (CheckingContact) -> Unit) {
            val contactWithId = checkingContact.contactWithId
            val contact = contactWithId.contact
            itemView.apply {
                contactNumber.text = contact.number
                contactName.text = contact.name
                tag = contactWithId.uid
                contactCheckBox?.apply {
                    isChecked = checkingContact.isChecked
                    setOnCheckedChangeListener { _, isChecked ->
                        checkAction(CheckingContact(contactWithId, isChecked))
                    }
                }
            }
        }
    }
}