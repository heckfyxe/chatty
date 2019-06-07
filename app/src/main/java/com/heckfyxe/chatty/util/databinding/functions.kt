package com.heckfyxe.chatty.util.databinding

import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.room.Message
import java.text.SimpleDateFormat
import java.util.*

private val formatter: SimpleDateFormat by lazy {
    SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val date: Date by lazy { Date() }

@BindingAdapter("hhmm")
fun hhmm(textView: TextView, time: Long) {
    textView.text = formatTime(time)
}

@BindingAdapter("outMessageText")
fun outMessageText(textView: TextView, message: Message) {
    if (message.sent)
        textView.text = formatTime(message.time)
    else
        textView.setText(R.string.sending)
}

private fun formatTime(time: Long): String {
    date.time = time
    return formatter.format(date)
}