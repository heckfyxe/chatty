package com.heckfyxe.chatty.model

import java.io.Serializable

data class User(
    var id: String,
    var name: String,
    var imageUrl: String
) : Serializable