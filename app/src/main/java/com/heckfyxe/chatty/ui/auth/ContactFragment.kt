package com.heckfyxe.chatty.ui.auth

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
import kotlinx.android.synthetic.main.contact_fragment.*
import org.koin.android.ext.android.inject

class ContactFragment : Fragment() {

    companion object {
        private const val RC_READ_CONTACTS_PERMISSION = 0
    }

    private val viewModel: ContactViewModel by inject()

    private lateinit var contactsAdapter: ContactsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactsAdapter = ContactsAdapter()

        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            connectViewModel()
            startProcess()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), RC_READ_CONTACTS_PERMISSION)
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
            supportActionBar?.title = getString(R.string.add_friends)
        }

        contactsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context!!)
            adapter = contactsAdapter
        }
    }

    private fun connectViewModel() {
        viewModel.contacts.observe(this, Observer {
            contactsProgressBar?.isVisible = false
            contactsAdapter.update(it)
        })

        viewModel.contactsCountLiveData.observe(this, Observer {
            contactsProgressBar?.max = it
        })

        viewModel.contactsProgress.observe(this, Observer {
            contactsProgressBar?.progress = it
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            RC_READ_CONTACTS_PERMISSION -> {
                val index = permissions.indexOf(Manifest.permission.READ_CONTACTS)
                if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                    startProcess()
                } else {
                    skipProcess()
                }
            }
        }
    }

    private fun startProcess() {
        contactsProgressBar?.isVisible = true
        viewModel.getUsers()
    }

    private fun skipProcess() {
        findNavController().navigate(R.id.action_contactFragment_to_mainFragment)
    }

}
