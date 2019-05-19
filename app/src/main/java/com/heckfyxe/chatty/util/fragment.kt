package com.heckfyxe.chatty.util

import android.content.Context
import androidx.fragment.app.Fragment

private const val KEY_IS_AUTHENTICATED = "com.heckfyxe.chatty.is_authenticated"

fun Fragment.isAuthenticated(): Boolean {
    val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
    return sharedPref?.getBoolean(KEY_IS_AUTHENTICATED, false) ?: false
}

fun Fragment.setAuthenticated() {
    activity?.getPreferences(Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_IS_AUTHENTICATED, true)?.apply()
}