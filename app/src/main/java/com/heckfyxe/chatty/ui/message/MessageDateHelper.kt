package com.heckfyxe.chatty.ui.message

import com.heckfyxe.chatty.model.Message
import java.util.*

fun getMessageTimeHeaders(messages: List<Message>): List<MessageTimeHeader> =
    messages.map { millisToDay(it.time) }.toSet().map {
        MessageTimeHeader(it)
    }


fun millisToDay(time: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
