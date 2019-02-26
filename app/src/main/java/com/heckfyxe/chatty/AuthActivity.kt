package com.heckfyxe.chatty

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    companion object {
        private const val RC_AUTH = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser != null) {
            startMainActivity()
            return
        }


        AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(
                listOf(
                    AuthUI.IdpConfig.PhoneBuilder()
                        .setDefaultCountryIso("ru")
                        .build()
                )
            )
            .build().let {
                startActivityForResult(it, RC_AUTH)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_AUTH -> {
                val response = IdpResponse.fromResultIntent(data)
                if (resultCode == Activity.RESULT_OK) {
                    startMainActivity()
                } else {
                    if (response == null)
                        finish()
                    else
                        Snackbar.make(window.decorView, "No Connection", Snackbar.LENGTH_SHORT).show()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startMainActivity() {
        Toast.makeText(this, "Successful", Toast.LENGTH_SHORT).show()
    }
}
