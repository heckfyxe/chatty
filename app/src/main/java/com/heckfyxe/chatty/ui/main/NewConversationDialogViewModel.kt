package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class NewConversationDialogViewModel: ViewModel(), KoinComponent {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val userDataCheckingChannel = Channel<String>(Channel.CONFLATED)

    val errors = MutableLiveData<Exception>()
    val result = MutableLiveData<Result>()

    private val usersRef: CollectionReference by inject("users")

    fun init() {
        scope.launch {
            for (data in userDataCheckingChannel) {
                usersRef
                    .whereEqualTo("phoneNumber", data)
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
        scope.launch { userDataCheckingChannel.send(data) }
    }

    fun createDialog(userId: String, success: (BaseChannel) -> Unit) {
        GroupChannel.createDistinctChannelIfNotExist(GroupChannelParams().addUserId(userId)) { channel, _, e ->
            if (e != null) {
                errors.postValue(e)
                return@createDistinctChannelIfNotExist
            }

            success(channel)
        }
    }

    class Result(val data: String, val userId: String?)
}