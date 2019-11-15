package com.heckfyxe.chatty.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.CheckingContact
import com.heckfyxe.chatty.model.Contact
import com.heckfyxe.chatty.model.ContactWithId
import com.heckfyxe.chatty.repository.ContactRepository
import com.heckfyxe.chatty.repository.UserRepository
import com.sendbird.android.SendBird
import com.sendbird.android.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class ContactViewModel : ViewModel(), KoinComponent {

    private val contactRepository: ContactRepository by inject()
    private val userRepository: UserRepository by inject()

    private val job = Job()
    private val scope = CoroutineScope(job)

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    val contactsCountLiveData = MutableLiveData<Int>()
    val contactsProgress = MutableLiveData<Int>()

    val errors = MutableLiveData<Error>()

    val friends = MutableLiveData<List<User>>()

    private var contactsCount: Int = 0
    private var loadedContactsCount: Int = 0
    private val contactsList = mutableListOf<ContactWithId>()

    private val checkedContactsIds = mutableSetOf<String>()

    private val channelJob = Job()
    private val channelScope = CoroutineScope(channelJob)
    private val channel = Channel<ContactStatus>(Channel.UNLIMITED)

    private var isLoading = false
    val isLoadingLiveData = MutableLiveData<Boolean>()

    val contacts = MutableLiveData<List<CheckingContact>>()

    init {
        channelScope.launch {
            for (contactStatus in channel) {
                loadedContactsCount++

                contactsProgress.postValue(loadedContactsCount)

                if (contactStatus.has) {
                    contactsList.add(contactStatus.contact)
                }

                if (loadedContactsCount == contactsCount) {
                    isLoading = false
                    isLoadingLiveData.postValue(false)
                    contacts.postValue(contactsList.map {
                        val isChecked = checkedContactsIds.contains(it.uid)
                        CheckingContact(it, isChecked)
                    })
                    prepareForGettingUsers()
                }
            }
        }
    }

    fun getUsers() {
        if (!isLoading) {
            isLoading = true
            isLoadingLiveData.postValue(true)
            scope.launch {
                val contacts = contactRepository.getContacts()
                contactsCount = contacts.size
                contactsCountLiveData.postValue(contactsCount)
                contacts.forEach {
                    checkPhoneNumber(it)
                }
            }
        }
    }

    private fun prepareForGettingUsers() {
        contactsCount = 0
        loadedContactsCount = 0
        contactsList.clear()
    }

    private fun checkPhoneNumber(contact: Contact) {
        usersRef.whereEqualTo("phoneNumber", contact.number)
            .limit(1)
            .get().addOnCompleteListener {
            scope.launch {
                if (!it.isSuccessful) {
                    channel.send(ContactStatus(ContactWithId(contact = contact), false))
                    return@launch
                }

                if (it.result?.isEmpty != true) {
                    channel.send(ContactStatus(ContactWithId(it.result!!.first().id, contact), true))
                } else {
                    channel.send(ContactStatus(ContactWithId(contact = contact), false))
                }
            }
        }
    }

    fun addFriendsToSendBird(ids: List<String>) {
        SendBird.addFriends(ids) { friendsList, e ->
            if (e != null) {
                errors.postValue(Error.ADD_FRIENDS_ERROR)
                return@addFriends
            }

            friends.postValue(friendsList)
        }
    }

    fun addUserToDatabase(
        users: List<com.heckfyxe.chatty.room.RoomUser>,
        onCompleteAction: () -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            userRepository.saveUsersInDatabase(users)
            onCompleteAction()
        }
    }

    fun addCheckingContact(checkingContact: CheckingContact) {
        val uid = checkingContact.contactWithId.uid
        if (checkingContact.isChecked) {
            checkedContactsIds.add(uid)
        } else {
            checkedContactsIds.remove(uid)
        }
        val contactList = contacts.value?.toMutableList() ?: return
        val index = contactList.indexOfFirst { it.contactWithId.uid == uid }
        contactList[index] = checkingContact
        contacts.postValue(contactList)
    }

    fun getCheckedContactsIds(): List<String> = checkedContactsIds.toList()

    data class ContactStatus(val contact: ContactWithId, val has: Boolean)

    enum class Error {
        CHECK_OF_NUMBER_ERROR, ADD_FRIENDS_ERROR
    }

    override fun onCleared() {
        super.onCleared()

        job.complete()
        channelJob.complete()
        channel.cancel()
    }
}
