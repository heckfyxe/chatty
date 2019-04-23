package com.heckfyxe.chatty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.Contact
import com.heckfyxe.chatty.repository.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class ContactViewModel : ViewModel(), KoinComponent {

    private val repository: ContactRepository by inject()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    val contactsCountLiveData = MutableLiveData<Int>()
    val contactsProgress = MutableLiveData<Int>()

    private var contactsCount: Int = 0
    private var loadedContactsCount: Int = 0
    private val contactsList = mutableListOf<Contact>()

    private val channel = Channel<ContactStatus>()

    val contacts = MutableLiveData<List<Contact>>()

    init {
        scope.launch {
            for (contactStatus in channel) {
                loadedContactsCount++

                contactsProgress.postValue(loadedContactsCount)

                if (contactStatus.has) {
                    contactsList.add(contactStatus.contact)
                }

                if (loadedContactsCount == contactsCount) {
                    contacts.postValue(contactsList)
                }
            }
        }
    }

    fun getUsers() = scope.launch {
        val contacts = repository.getContacts()
        contactsCount = contacts.size
        contactsCountLiveData.postValue(contactsCount)
        contacts.forEach {
            checkPhoneNumber(it)
        }
    }

    private fun checkPhoneNumber(contact: Contact) {
        usersRef.whereEqualTo("phoneNumber", contact.number).get().addOnCompleteListener {
            scope.launch {
                if (!it.isSuccessful) {
                    channel.send(ContactStatus(contact, false))
                    return@launch
                }

                channel.send(ContactStatus(contact, it.result?.isEmpty != true))
            }
        }
    }

    data class ContactStatus(val contact: Contact, val has: Boolean)

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        channel.cancel()
    }
}
