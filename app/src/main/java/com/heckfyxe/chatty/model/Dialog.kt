package com.heckfyxe.chatty.model

import com.sendbird.android.GroupChannel
import com.stfalcon.chatkit.commons.models.IDialog

class Dialog(private val channel: GroupChannel, private val currentUserId: String): IDialog<Message> {

    private var lastMessage: Message = Message(channel.lastMessage)

    override fun getDialogPhoto(): String {
        if (isPersonal()) {
            return getPersonalMember().profileUrl
        }

        return channel.coverUrl
    }

    override fun getUnreadCount(): Int = channel.unreadMessageCount

    override fun setLastMessage(message: Message) {
        lastMessage = message
    }

    override fun getId(): String = channel.url

    override fun getUsers(): List<User> = channel.members.map {
        User(it.userId, it.nickname, it.profileUrl)
    }

    override fun getLastMessage(): Message = Message(channel.lastMessage)

    override fun getDialogName(): String {
        if (isPersonal()) {
            return getPersonalMember().nickname
        }

        return channel.name
    }

    private fun isPersonal() =
        channel.isDistinct && channel.isGroupChannel && channel.memberCount == 2

    private fun getPersonalMember() =
        channel.members.single { it.userId != currentUserId }

}