package com.heckfyxe.chatty.repository

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.heckfyxe.chatty.remote.SendBirdApi
import com.heckfyxe.chatty.util.sendbird.toDomain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class EditUserDataRepository(
    private val sendBirdApi: SendBirdApi,
    private val usersRef: CollectionReference
) {

    @Volatile
    private var isConnected: Boolean = false
        get() = synchronized(this) {
            field
        }
        set(value) = synchronized(this) {
            field = value
        }

    suspend fun connect() =
        sendBirdApi.connect().toDomain().also {
            isConnected = true
        }

    suspend fun checkNickname(nickname: String): Boolean = suspendCancellableCoroutine { cont ->
        usersRef.whereEqualTo("nickname", nickname).limit(1).get(Source.SERVER)
            .addOnCompleteListener {
                if (!it.isSuccessful) {
                    cont.cancel(it.exception)
                    return@addOnCompleteListener
                }
                cont.resume(it.result == null || it.result!!.isEmpty)
            }
    }

    suspend fun updateUserInfo(userId: String, nickname: String) =
        coroutineScope<Unit> {
            if (!isConnected) connect()
            val firebaseNicknameUpdating = async {
                updateNicknameFirebase(userId, nickname)
            }
            val nicknameUpdating = async {
                sendBirdApi.updateNickname(nickname)
            }
            awaitAll(firebaseNicknameUpdating, nicknameUpdating)
        }

    suspend fun updateUserInfo(userId: String, nickname: String, avatarImage: File) =
        coroutineScope<Unit> {
            if (!isConnected) connect()
            val firebaseNicknameUpdating = async {
                updateNicknameFirebase(userId, nickname)
            }
            val nicknameAndAvatarUpdating = async {
                sendBirdApi.updateNicknameWithAvatarImage(nickname, avatarImage)
            }
            awaitAll(firebaseNicknameUpdating, nicknameAndAvatarUpdating)
        }

    private suspend fun updateNicknameFirebase(userId: String, nickname: String) =
        suspendCancellableCoroutine<Unit> { cont ->
            usersRef.document(userId).set(mapOf("nickname" to nickname), SetOptions.merge())
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        cont.resume(Unit)
                    } else {
                        cont.cancel(it.exception)
                    }
                }
        }
}
