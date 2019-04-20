package com.heckfyxe.chatty.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.model.Contact
import com.heckfyxe.chatty.repository.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class ContactViewModel : ViewModel(), KoinComponent {

    private val repository: ContactRepository by inject()

    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    fun getUsers() = scope.launch {
        val contacts = repository.getContacts()
        var query: Query? = null
        contacts.forEach {
            query = if (query != null)
                query!!.whereEqualTo("phoneNumber", it.number)
            else
                usersRef.whereEqualTo("phoneNumber", it.number)
        }

        query?.get()?.addOnCompleteListener {
            if (!it.isSuccessful) {
                Log.w("ContactViewModel", "isn't successful", it.exception)
                return@addOnCompleteListener
            }

            val users = it.result?.documents?.map { doc ->
                Contact(doc.getString("phoneNumber")!!, doc.getString("nickname")!!)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        job.cancel()
    }
}
