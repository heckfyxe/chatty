package com.heckfyxe.chatty

import android.graphics.ImageFormat
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import com.heckfyxe.chatty.koin.isUserScopeInitialized
import com.heckfyxe.chatty.koin.userScope
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity(), EmotionDetector {

    private val settings: FirebaseVisionFaceDetectorOptions by lazy {
        FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()
    }

    private val detector: FirebaseVisionFaceDetector by lazy {
        FirebaseVision.getInstance().getVisionFaceDetector(settings)
    }

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)
    private val uid: String by lazy { userScope.get<String>(KOIN_USER_ID) }

    private var emotion = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView?.setLifecycleOwner(this)
    }

    override fun start() {
        var isLoading = false
        cameraView?.addFrameProcessor {
            if (isLoading) return@addFrameProcessor
            if (!isUserScopeInitialized) return@addFrameProcessor
            isLoading = true

            val metadata = FirebaseVisionImageMetadata.Builder()
                .setFormat(ImageFormat.NV21)
                .setWidth(it.size.width)
                .setHeight(it.size.height)
                .build()

            val image = FirebaseVisionImage.fromByteArray(it.data, metadata)

            detector.detectInImage(image).addOnCompleteListener { task ->
                isLoading = false
                if (!task.isSuccessful) {
                    Log.e("Smiling", "error", task.exception)
                    return@addOnCompleteListener
                }

                val faces = task.result
                val face = faces?.singleOrNull() ?: return@addOnCompleteListener
                Log.i("Smiling", face.smilingProbability.toString())
                val isSmiling = face.smilingProbability > 0.5f
                val emotion = if (isSmiling) "\uD83D\uDE04" else " "
                if (this.emotion != emotion) {
                    this.emotion = emotion
                    usersRef.document(uid).set(mapOf("emotion" to emotion), SetOptions.merge())
                }
            }
        }
    }
}

interface EmotionDetector {
    fun start()
}
