package com.heckfyxe.chatty.util

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

private const val KEY_IS_AUTHENTICATED = "com.heckfyxe.chatty.is_authenticated"

fun Fragment.isAuthenticated(): Boolean {
    val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
    return sharedPref?.getBoolean(KEY_IS_AUTHENTICATED, false) ?: false
}

fun Fragment.setAuthenticated() {
    activity?.getPreferences(Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_IS_AUTHENTICATED, true)?.apply()
}

fun Fragment.clearSharedPreferencesData() {
    activity?.getPreferences(Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
}

fun Fragment.snackbar(@StringRes text: Int) {
    Snackbar.make(activity!!.window.decorView, text, Snackbar.LENGTH_SHORT).show()
}

fun Fragment.snackbarLong(@StringRes text: Int) {
    Snackbar.make(activity!!.window.decorView, text, Snackbar.LENGTH_LONG).show()
}

fun Fragment.snackbar(@StringRes text: Int, @StringRes actionText: Int, action: (View) -> Unit) {
    Snackbar.make(activity!!.window.decorView, text, Snackbar.LENGTH_INDEFINITE)
        .setAction(actionText, action)
        .show()
}