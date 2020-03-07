package com.heckfyxe.chatty.util

import android.content.res.Resources
import android.util.TypedValue

inline val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )