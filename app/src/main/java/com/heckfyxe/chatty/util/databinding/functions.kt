package com.heckfyxe.chatty.util.databinding

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.ui.main.DialogsAdapter
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
fun loadAvatar(imageView: ImageView?, data: Any?) {
    imageView ?: return
    data ?: return

    Glide.with(imageView)
        .load(data)
        .transition(
            withCrossFade(
                DrawableCrossFadeFactory.Builder()
                    .setCrossFadeEnabled(true)
                    .build()
            )
        ).transform(RoundedCorners(20))
        .placeholder(R.drawable.ic_user)
        .error(R.drawable.ic_broken_image)
        .into(imageView)
}

@BindingAdapter("dialogItems")
fun dialogItems(recyclerView: RecyclerView?, dialogs: List<Dialog>?) {
    dialogs ?: return
    (recyclerView?.adapter as? DialogsAdapter)?.submitList(dialogs)
}
