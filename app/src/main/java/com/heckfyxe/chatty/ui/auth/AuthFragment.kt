package com.heckfyxe.chatty.ui.auth


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.ConfigurationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.util.isAuthenticated


class AuthFragment : Fragment() {

    private lateinit var locale: String

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

        if (isAuthenticated()) {
            return
        }

        locale = ConfigurationCompat.getLocales(resources.configuration)[0].country
        authPhoneByPhoneNumber()
    }

    private fun authPhoneByPhoneNumber() {
        AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(
                listOf(
                    AuthUI.IdpConfig.PhoneBuilder()
                        .setDefaultCountryIso(locale)
                        .build()
                )
            )
            .build().let {
                startActivityForResult(it, RC_AUTH)
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isAuthenticated())
            startMainActivity()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_AUTH -> {
                val response = IdpResponse.fromResultIntent(data) ?: return

                if (resultCode == Activity.RESULT_OK) {
                    startMainActivity(response.isNewUser)
                } else {
                    val view = activity?.findViewById<View>(android.R.id.content)
                    Snackbar.make(view!!, R.string.no_connection, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.retry) {
                            authPhoneByPhoneNumber()
                        }
                        .show()

                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startMainActivity(isNewUser: Boolean = false) {
        if (isNewUser)
            findNavController().navigate(R.id.action_authFragment_to_editUserDataFragment)
        else
            findNavController().navigate(R.id.action_authFragment_to_mainFragment)
    }
}
