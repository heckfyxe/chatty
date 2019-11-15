package com.heckfyxe.chatty.util

class OnClickAction<T : Any>(private val clickAction: (T) -> Unit) {
    fun action(model: T) = clickAction(model)
}