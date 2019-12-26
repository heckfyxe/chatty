package com.heckfyxe.chatty.util.databinding

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.Dialog
import com.heckfyxe.chatty.model.Message
import com.heckfyxe.chatty.ui.main.DialogsAdapter
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private val formatter: SimpleDateFormat by lazy {
    SimpleDateFormat("HH:mm", Locale.US)
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

@BindingAdapter("loadOutMessageImage")
fun loadOutImage(imageView: ImageView?, image: Any?) {
    loadImage(
        imageView, image, RoundedCornersTransformation(
            20,
            0,
            RoundedCornersTransformation.CornerType.OTHER_BOTTOM_RIGHT
        )
    )
}

@BindingAdapter("loadInMessageImage")
fun loadInImage(imageView: ImageView?, image: Any?) {
    loadImage(
        imageView, image, RoundedCornersTransformation(
            20,
            0,
            RoundedCornersTransformation.CornerType.OTHER_BOTTOM_LEFT
        )
    )
}

private fun loadImage(imageView: ImageView?, image: Any?, transformation: Transformation<Bitmap>) {
    imageView ?: return
    image ?: return

    val displayMetrics = imageView.resources.displayMetrics
    val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200f, displayMetrics)

    imageView.layoutParams.apply {
        width = size.roundToInt()
        height = size.roundToInt()
    }
    imageView.adjustViewBounds = false

    Glide.with(imageView)
        .load(image)
        .transition(
            withCrossFade(
                DrawableCrossFadeFactory.Builder()
                    .setCrossFadeEnabled(true)
                    .build()
            )
        )
        .centerCrop()
        .transform(transformation)
        .error(R.drawable.ic_broken_image)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?, model: Any?, target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean = false

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                imageView.adjustViewBounds = true
                imageView.layoutParams.apply {
                    width = WRAP_CONTENT
                    height = WRAP_CONTENT
                }
                return false
            }
        })
        .into(imageView)
}

@BindingAdapter("dialogItems")
fun dialogItems(recyclerView: RecyclerView?, dialogs: List<Dialog>?) {
    dialogs ?: return
    (recyclerView?.adapter as? DialogsAdapter)?.submitList(dialogs)
}

@BindingAdapter("avatarTransitionName")
fun avatarTransitionName(imageView: ImageView?, dialogId: String?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        imageView?.transitionName = "${dialogId}avatar"
}

@BindingAdapter("nicknameTransitionName")
fun nicknameTransitionName(textView: TextView?, dialogId: String?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        textView?.transitionName = "${dialogId}nickname"
}