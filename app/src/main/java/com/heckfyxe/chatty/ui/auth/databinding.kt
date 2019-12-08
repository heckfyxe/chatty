package com.heckfyxe.chatty.ui.auth

import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("helperTextColor")
fun helperTextColor(editText: TextInputLayout, @ColorRes color: Int?) {
    color ?: return
    editText.setHelperTextColor(ContextCompat.getColorStateList(editText.context, color))
}

@BindingAdapter("showLoading")
fun showLoading(button: CircularProgressButton?, status: Status?) {
    when (status) {
        UpdatingData -> button?.startAnimation()
        DataUpdated -> button?.stopAnimation()
        is Error -> if (status.type == ErrorType.UPDATE_USER_DATA) {
//            button?.stopAnimation()
            button?.revertAnimation()
        }
    }
}