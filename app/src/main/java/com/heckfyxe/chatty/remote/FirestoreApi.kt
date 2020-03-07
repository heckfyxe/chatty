package com.heckfyxe.chatty.remote

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val USERS_COLLECTION = "users"

class FirestoreApi {

    private val firestore = FirebaseFirestore.getInstance()
    private val users = firestore.collection(USERS_COLLECTION)

    suspend fun getUserIdByPhoneNumber(number: String) =
        suspendCancellableCoroutine<String?> { cont ->
            users.whereEqualTo("phoneNumber", number)
                .limit(1)
                .get().addOnCompleteListener {
                    if (!it.isSuccessful) {
                        cont.cancel(it.exception)
                        return@addOnCompleteListener
                    }

                    if (it.result?.isEmpty != true) {
                        cont.resume(it.result!!.first().id)
                        return@addOnCompleteListener
                    }

                    cont.resume(null)
                }
        }
}