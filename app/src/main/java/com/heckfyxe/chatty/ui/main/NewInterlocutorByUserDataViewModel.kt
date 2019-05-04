package com.heckfyxe.chatty.ui.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class NewInterlocutorByUserDataViewModel(private val userDataName: String) : ViewModel(), KoinComponent {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val userDataCheckingChannel = Channel<String>(Channel.CONFLATED)

    val userId: String by inject(KOIN_USER_ID)

    val errors = MutableLiveData<Exception>()
    val result = MutableLiveData<Result>()

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    init {
        scope.launch {
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

    override fun onCleared() {
        super.onCleared()

        job.cancel()
        userDataCheckingChannel.close()
    }

    class Result(val data: String, val userId: String?)

}