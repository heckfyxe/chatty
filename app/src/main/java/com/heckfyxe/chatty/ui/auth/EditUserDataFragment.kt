package com.heckfyxe.chatty.ui.auth

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.databinding.EditUserDataFragmentBinding
import com.heckfyxe.chatty.util.setAuthenticated
import kotlinx.android.synthetic.main.edit_user_data_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val EXTRA_NICKNAME = "com.heckfyxe.chatty.EXTRA_NICKNAME"
private const val EXTRA_PHOTO_FILE = "com.heckfyxe.chatty.EXTRA_PHOTO_FILE"

private const val RC_TAKE_PHOTO = 0

class EditUserDataFragment : Fragment() {

    private val editUserDataViewModel: EditUserDataViewModel by viewModel()

    private var avatarImageFile: File? = null
    private var isPhotoTaken = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editUserDataViewModel.init()
        observeViewModel()
    }

    private fun observeViewModel() {
        editUserDataViewModel.status.observe(this, Observer { status ->
            when (status) {
                is DataUpdated -> {
                    setAuthenticated()
                    findNavController().navigate(R.id.action_editUserDataFragment_to_contactFragment)
                    editUserDataViewModel.onDataUpdated()
                }
                is Error -> if (status.type == ErrorType.UPDATE_USER_DATA) {
                    Snackbar.make(
                        editUserDataFragmentRoot,
                        R.string.data_updating_error,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        })

        editUserDataViewModel.takePhotoEvent.observe(this, Observer {
            if (it != true) return@Observer
            editUserDataViewModel.onTakePhoto()

            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(context!!.packageManager)?.also {
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (e: IOException) {
                        Toast.makeText(context!!, R.string.error, Toast.LENGTH_SHORT).show()
                        return@Observer
                    }
                    photoFile?.also { file ->
                        val photoURI: Uri = FileProvider.getUriForFile(
                            context!!,
                            "com.heckfyxe.chatty.fileprovider",
                            file
                        )
                        avatarImageFile = file
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(takePictureIntent, RC_TAKE_PHOTO)
                    }
                }
            }
        })
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = EditUserDataFragmentBinding.inflate(inflater).run {
        lifecycleOwner = this@EditUserDataFragment
        viewModel = editUserDataViewModel

        nicknameEditText.apply {
            addTextChangedListener {
                if (!it.isNullOrBlank()) {
                    editUserDataViewModel.checkNickname(it.toString())
                }
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE &&
                    editUserDataViewModel.status.value is NicknameReady
                ) {
                    editUserDataViewModel.updateUserData()
                    true
                } else
                    false
            }
        }

        avatarImageView.setOnClickListener {
            editUserDataViewModel.takePhoto()
        }

        nicknameOkButton.apply {
            saveInitialState()
            setOnClickListener {
                editUserDataViewModel.updateUserData()
            }
        }

        return root
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_TAKE_PHOTO -> {
                if (resultCode != RESULT_OK) return

                isPhotoTaken
                editUserDataViewModel.onPhotoTaken(avatarImageFile!!)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState?.apply {
            val nickname = getCharSequence(EXTRA_NICKNAME)?.toString()
            if (nickname != null) {
                nicknameEditText?.setText(nickname)
                editUserDataViewModel.checkNickname(nickname)
            }
            avatarImageFile = getSerializable(EXTRA_PHOTO_FILE) as? File
            editUserDataViewModel.onPhotoTaken(avatarImageFile ?: return)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putCharSequence(EXTRA_NICKNAME, nicknameEditText?.text)
            putSerializable(EXTRA_PHOTO_FILE, avatarImageFile)
        }
    }
}