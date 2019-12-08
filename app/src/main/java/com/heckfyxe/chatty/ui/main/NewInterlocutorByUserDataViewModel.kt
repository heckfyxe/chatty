package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.remote.SendBirdApi
import com.sendbird.android.GroupChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class NewInterlocutorByUserDataViewModel(private val userDataName: String) : ViewModel(),
    KoinComponent {

    private val userDataCheckingChannel = Channel<String>(Channel.CONFLATED)

    val userId: String by inject(KOIN_USER_ID)

    private val sendBirdApi: SendBirdApi by inject()

    val errors = MutableLiveData<Exception>()
    val result = MutableLiveData<Result>()

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    init {
        viewModelScope.launch {
            for (data in userDataCheckingChannel) {
                usersRef
                    .whereEqualTo(userDataName, data)
                    .limit(1)
                    .get()
                    .addOnCompleteListener {
                        if (!it.isSuccessful) {
                            errors.postValue(it.exception)
                            return@addOnCompleteListener
                        }

                        if (it.result?.isEmpty == true) {
                            result.postValue(Result(data, null))
                        } else {
                            result.postValue(Result(data, it.result!!.documents.first().id))
                        }
                    }
            }
        }
    }

    fun checkData(data: String) {
        viewModelScope.launch { userDataCheckingChannel.send(data) }
    }

    fun createDialog(interlocutorId: String, success: (GroupChannel) -> Unit) {
        viewModelScope.launch {
            try {
                val channel = sendBirdApi.createChannel(interlocutorId)
                success(channel)
            } catch (e: Exception) {
                errors.value = e
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        userDataCheckingChannel.close()
    }

    class Result(val data: String, val userId: String?)
}