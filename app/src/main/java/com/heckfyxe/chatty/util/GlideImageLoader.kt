package com.heckfyxe.chatty.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.stfalcon.chatkit.commons.ImageLoader

class GlideImageLoader : ImageLoader {

    override fun loadImage(imageView: ImageView, url: String?, payload: Any?) {
        Glide.with(imageView)
            .load(url)
            .into(imageView)
    }
}