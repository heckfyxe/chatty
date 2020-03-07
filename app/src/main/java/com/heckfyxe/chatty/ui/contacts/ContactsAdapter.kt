package com.heckfyxe.chatty.ui.contacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.User
import kotlinx.android.synthetic.main.item_contact.view.*

class ContactsAdapter(private val action: (User) -> Unit) :
    RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private val contactList = mutableListOf<UserContact>()

    class ContactDiff(
        private val oldList: List<UserContact>,
        private val newList: List<UserContact>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].user.id == newList[newItemPosition].user.id

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    fun update(list: List<UserContact>) {
        val diff = DiffUtil.calculateDiff(
            ContactDiff(
                contactList,
                list
            )
        )
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
        holder.bind(contactList[position], action)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(userInfo: UserContact, action: (User) -> Unit) {
            val contact = userInfo.contact
            itemView.apply {
                contactNumber.text = contact.number
                contactName.text = contact.name
                setOnClickListener {
                    action(userInfo.user)
                }
            }
        }
    }
}