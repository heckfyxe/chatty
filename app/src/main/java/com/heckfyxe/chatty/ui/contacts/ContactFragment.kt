package com.heckfyxe.chatty.ui.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.util.snackbar
import kotlinx.android.synthetic.main.contact_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ContactFragment : Fragment() {

    companion object {
        private const val RC_READ_CONTACTS_PERMISSION = 0
    }

    private val viewModel: ContactViewModel by viewModel()

    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactsAdapter = ContactsAdapter {
            goToMessageFragment(it)
        }

        connectViewModel()

        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (savedInstanceState == null)
                startProcess()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                RC_READ_CONTACTS_PERMISSION
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.contact_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(contactToolbar)
        }

        contactsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context!!)
            adapter = contactsAdapter
        }
    }

    private fun connectViewModel() {
        viewModel.contacts.observe(this, Observer {
            contactsAdapter.update(it)
        })

        viewModel.contactsCount.observe(this, Observer {
            contactsProgressBar?.max = it
            if (it == 0) {
                // TODO("Display no contacts")
            }
        })

        viewModel.progress.observe(this, Observer {
            contactsProgressBar?.progress = it
        })

        viewModel.isLoading.observe(this, Observer {
            contactsProgressBar?.isVisible = it
            contactsRecyclerView?.isVisible = !it
        })

        viewModel.errors.observe(this, Observer {
            it ?: return@Observer
            when (it) {
                Error.CHECK_OF_NUMBER_ERROR -> {
                    snackbar(R.string.data_updating_error, R.string.retry) {
                        viewModel.loadUsersAgain()
                    }
                }
            }
            viewModel.onErrorsCaught()
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            RC_READ_CONTACTS_PERMISSION -> {
                val index = permissions.indexOf(Manifest.permission.READ_CONTACTS)
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    startProcess()
                } else {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun startProcess() {
        viewModel.loadUsers()
    }

    private fun goToMessageFragment(interlocutor: User) {
        findNavController().navigate(
            ContactFragmentDirections.actionContactFragmentToMessageFragment(
                null,
                interlocutor,
                -1,
                null
            )
        )
    }

}
