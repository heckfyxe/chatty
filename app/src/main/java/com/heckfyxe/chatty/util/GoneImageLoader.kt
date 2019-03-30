package com.heckfyxe.chatty.util

import android.widget.ImageView
import androidx.core.view.isGone
import com.stfalcon.chatkit.commons.ImageLoader

class GoneImageLoader : ImageLoader {

    override fun loadImage(imageView: ImageView?, url: String?, payload: Any?) {
        imageView?.isGone = true
    }
}