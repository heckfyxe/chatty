package com.heckfyxe.chatty.model

import com.sendbird.android.User
import com.stfalcon.chatkit.commons.models.IUser

class User: IUser {

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

    fun setId(id: String) {
        _id = id
    }

    override fun getName(): String = _nickname

    fun setName(name: String) {
        _nickname = name
    }

    override fun getAvatar(): String? = _avatar

    fun setAvatar(avatarUrl: String?) {
        _avatar = avatarUrl
    }
}