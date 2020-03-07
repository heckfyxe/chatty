package com.heckfyxe.chatty.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.koin.userScope
import com.heckfyxe.chatty.remote.SendBirdApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import kotlin.coroutines.resume

class EditUserDataRepository : KoinComponent {

    private val sendBirdApi: SendBirdApi by userScope.inject()
    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)

    suspend fun getAvatarUrl(): String = sendBirdApi.getCurrentUser().profileUrl

    suspend fun checkNickname(nickname: String): Boolean = suspendCancellableCoroutine { cont ->
        if (nickname.isBlank()) cont.resume(false)
        usersRef.whereEqualTo("nickname", nickname).limit(1).get(Source.SERVER)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    cont.cancel(it.exception)
                    return@addOnCompleteListener
                }
                cont.resume(it.result == null || it.result!!.isEmpty)
            }
    }

    suspend fun updateUserInfo(userId: String, nickname: String, phoneNumber: String) {
        sendBirdApi.updateNickname(nickname)
        updateNicknameFirebase(userId, nickname, phoneNumber)
    }

    suspend fun updateUserInfo(
        userId: String,
        nickname: String,
        phoneNumber: String,
        avatarImage: File
    ) {
        sendBirdApi.updateNicknameWithAvatarImage(nickname, avatarImage)
        updateNicknameFirebase(userId, nickname, phoneNumber)
    }

    private suspend fun updateNicknameFirebase(
        userId: String,
        nickname: String,
        phoneNumber: String
    ) = suspendCancellableCoroutine<Unit> { cont ->
        usersRef.document(userId).set(
            mapOf("nickname" to nickname, "phoneNumber" to phoneNumber),
            SetOptions.merge()
        ).addOnCompleteListener {
            if (it.isSuccessful) {
                cont.resume(Unit)
            } else {
                cont.cancel(it.exception)
            }
        }
    }
}
