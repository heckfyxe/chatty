package com.heckfyxe.chatty.model

import com.sendbird.android.User
import com.stfalcon.chatkit.commons.models.IUser

class ChatUser : IUser {

    private var _id: String
    private var _nickname: String
    private var _avatar: String?

    constructor(user: User): this(user.userId, user.nickname, user.profileUrl)

    constructor(id: String, nickname: String, avatar: String) {
        _id = id
        _nickname = nickname
        _avatar = avatar
    }

    override fun getId(): String = _id

    override fun getName(): String = _nickname

    override fun getAvatar(): String? = _avatar
}