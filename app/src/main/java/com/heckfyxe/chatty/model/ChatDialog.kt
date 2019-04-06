package com.heckfyxe.chatty.model

import com.sendbird.android.GroupChannel
import com.stfalcon.chatkit.commons.models.IDialog

class ChatDialog : IDialog<ChatMessage> {

    private var _id: String
    private var _name: String
    private var _photo: String
    private var _lastMessage: ChatMessage
    private var _unreadCount: Int

    constructor(id: String, name: String, photo: String, lastMessage: ChatMessage, unreadCount: Int) {
        _id = id
        _name = name
        _photo = photo
        _lastMessage = lastMessage
        _unreadCount = unreadCount
    }

    constructor(channel: GroupChannel, currentUserId: String) :
            this(
                channel.url,
                channel.name,
                channel.members.single { it.userId != currentUserId }.profileUrl,
                ChatMessage(channel.lastMessage),
                channel.unreadMessageCount
            )

    override fun getId(): String = _id

    override fun getDialogName(): String = _name

    override fun getDialogPhoto(): String = _photo

    override fun getLastMessage(): ChatMessage = _lastMessage

    override fun setLastMessage(message: ChatMessage) {
        _lastMessage = message
    }

    override fun getUnreadCount(): Int = _unreadCount

    override fun getUsers(): List<ChatUser> = listOf()
}