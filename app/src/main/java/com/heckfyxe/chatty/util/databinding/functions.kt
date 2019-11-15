package com.heckfyxe.chatty.util.databinding

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import coil.api.load
import coil.transform.RoundedCornersTransformation
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Message
import java.text.SimpleDateFormat
import java.util.*

private val formatter: SimpleDateFormat by lazy {
    SimpleDateFormat("HH:mm", Locale.getDefault())
}

private val date: Date by lazy { Date() }

@BindingAdapter("hhmm")
fun hhmm(textView: TextView, time: Long?) {
    if (time == null)
        return

    textView.text = formatTime(time)
}

@BindingAdapter("outMessageText")
fun outMessageText(textView: TextView, message: Message?) {
    if (message == null)
        return

    if (message.sent)
        textView.text = formatTime(message.time)
    else
        textView.setText(R.string.sending)
}

private fun formatTime(time: Long): String {
    date.time = time
    return formatter.format(date)
}

@BindingAdapter("loadAvatar")
fun loadAvatar(imageView: ImageView?, url: String?) {
    imageView?.load(url) {
        transformations(RoundedCornersTransformation(20f))
        build()
    }
}
