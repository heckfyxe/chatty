package com.heckfyxe.chatty.ui.auth


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.heckfyxe.chatty.R


class AuthFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_auth, container, false)
    }


    companion object {
        private const val RC_AUTH = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser != null) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser != null)
            startMainActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_AUTH -> {
                val response = IdpResponse.fromResultIntent(data)
                if (resultCode == Activity.RESULT_OK) {
                    startMainActivity(response!!.isNewUser)
                } else {
                    if (response == null)
                        activity?.finish()
                    else {
                        val view = activity?.findViewById<View>(android.R.id.content)
                        Snackbar.make(view!!, R.string.no_connection, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startMainActivity(isNewUser: Boolean = false) {
        if (isNewUser)
            TODO("Fill user data")
        else
            findNavController().navigate(R.id.action_authFragment_to_mainFragment)
    }
}
