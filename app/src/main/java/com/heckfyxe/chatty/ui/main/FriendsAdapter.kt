package com.heckfyxe.chatty.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heckfyxe.chatty.R
import com.sendbird.android.User
import kotlinx.android.synthetic.main.item_friend.view.*

class FriendsAdapter(private val onClickAction: (User) -> Unit) :
    RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    private val friends = mutableListOf<User>()

    class FriendsDiff(private val oldList: List<User>, private val newList: List<User>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.userId == new.userId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old == new
        }

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
    }

    fun update(friendList: List<User>) {
        val diff = DiffUtil.calculateDiff(FriendsDiff(friends, friendList))
        friends.clear()
        friends.addAll(friendList)
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_friend, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = friends.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(friends[position], onClickAction)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(friend: User, onClickAction: (User) -> Unit) {
            itemView.apply {
                friendNickname?.text = friend.nickname
                friendPhoneNumber?.text = friend.metaData["phoneNumber"]
                setOnClickListener { onClickAction(friend) }
            }
        }
    }
}