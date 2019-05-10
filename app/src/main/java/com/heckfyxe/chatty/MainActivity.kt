package com.heckfyxe.chatty

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.heckfyxe.chatty.emotion.EmotionRecognition
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        EmotionRecognition.newInstance(this, surfaceView)
    }
}
