package com.heckfyxe.chatty.ui.contacts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckfyxe.chatty.koin.userScope
import com.heckfyxe.chatty.model.Contact
import com.heckfyxe.chatty.remote.FirestoreApi
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.repository.ContactRepository
import com.heckfyxe.chatty.util.sendbird.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject

internal sealed class Status
internal data class ContactStatus(val userInfo: UserContact?, val has: Boolean) : Status()
internal object Reset : Status()

enum class Error {
    CHECK_OF_NUMBER_ERROR
}

class ContactViewModel : ViewModel(), KoinComponent {

    private val contactRepository: ContactRepository by inject()

    private val firestoreApi: FirestoreApi by inject()
    private val sendBirdApi: SendBirdApi by userScope.inject()

    private val _contactsCount = MutableLiveData<Int>()
    val contactsCount: LiveData<Int> = _contactsCount

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress

    private val _errors = MutableLiveData<Error>()
    val errors: LiveData<Error> = _errors

    private val channel = Channel<Status>()

    private var _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _contacts = MutableLiveData<List<UserContact>>()
    val contacts: LiveData<List<UserContact>> = _contacts

    init {
        _isLoading.value = false

        viewModelScope.launch {
            var loadedContactsCount = 0
            val contactsList = mutableListOf<UserContact>()
            fun resetVars() {
                _isLoading.value = false
                _contacts.value = contactsList
                _contactsCount.value = 0
                _progress.value = 0
                loadedContactsCount = 0
                contactsList.clear()
            }
            for (contactStatus in channel) {
                when (contactStatus) {
                    is Reset -> resetVars()
                    is ContactStatus -> {
                        loadedContactsCount++

                        _progress.value = loadedContactsCount

                        if (contactStatus.has) {
                            contactsList.add(contactStatus.userInfo!!)
                        }

                        if (loadedContactsCount == contactsCount.value) {
                            resetVars()
                        }
                    }
                }
            }
        }
    }

    fun loadUsersAgain() = viewModelScope.launch {
        channel.send(Reset)
        loadUsers()
    }

    fun loadUsers() {
        if (_isLoading.value == false) {
            _isLoading.value = true
            viewModelScope.launch {
                val contacts = withContext(Dispatchers.IO) { contactRepository.getContacts() }
                _contactsCount.value = contacts.size
                val job = Job()
                for (contact in contacts) {
                    try {
                        viewModelScope.launch(job) {
                            checkPhoneNumber(contact)
                        }
                    } catch (e: Exception) {
                        _errors.value = Error.CHECK_OF_NUMBER_ERROR
                        break
                    }
                }
            }
        }
    }

    private suspend fun checkPhoneNumber(contact: Contact) {
        val userId = firestoreApi.getUserIdByPhoneNumber(contact.number)
        if (userId == null) {
            channel.send(ContactStatus(null, false))
            return
        }
        val user = sendBirdApi.getUserById(userId)?.toDomain()
        if (user == null) {
            channel.send(ContactStatus(null, false))
            return
        }
        channel.send(ContactStatus(UserContact(user, contact), true))
    }

    fun onErrorsCaught() {
        _errors.value = null
    }

    override fun onCleared() {
        super.onCleared()

        channel.cancel()
    }
}
