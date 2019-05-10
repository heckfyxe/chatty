package com.heckfyxe.chatty.emotion

import android.content.Context
import android.view.SurfaceView
import com.affectiva.android.affdex.sdk.Frame
import com.affectiva.android.affdex.sdk.detector.CameraDetector
import com.affectiva.android.affdex.sdk.detector.Detector
import com.affectiva.android.affdex.sdk.detector.Face
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.SetOptions
import com.heckfyxe.chatty.koin.KOIN_USERS_FIRESTORE_COLLECTION
import com.heckfyxe.chatty.koin.KOIN_USER_ID
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

class EmotionRecognition(context: Context, surfaceView: SurfaceView) : Detector.ImageListener, KoinComponent {

    private val usersRef: CollectionReference by inject(KOIN_USERS_FIRESTORE_COLLECTION)
    private val userId: String by inject(KOIN_USER_ID)

    private val detector: Detector = CameraDetector(
        context, CameraDetector.CameraType.CAMERA_FRONT,
        surfaceView, 1, Detector.FaceDetectorMode.LARGE_FACES
    ).apply {
        setDetectAllEmojis(true)
        setImageListener(this@EmotionRecognition)
    }

    fun start() = detector.start()

    fun isRunning() = detector.isRunning

    fun stop() = detector.stop()

    override fun onImageResults(faces: MutableList<Face>?, p1: Frame?, p2: Float) {
        if (faces.isNullOrEmpty()) {
            return
        }

        val face = faces.singleOrNull() ?: return
        usersRef.document(userId).set(mapOf("emotion" to face.emojis.dominantEmoji.unicode), SetOptions.merge())
    }

    companion object {
        private var recognition: EmotionRecognition? = null

        fun newInstance(context: Context, surfaceView: SurfaceView) {
            if (recognition == null)
                recognition = EmotionRecognition(context, surfaceView)
        }

        fun getInstance(): EmotionRecognition {
            if (recognition == null)
                throw Exception("EmotionRecognition must be initialized")

            return recognition!!
        }
    }
}