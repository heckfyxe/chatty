package com.heckfyxe.chatty.ui.auth

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.databinding.EditUserDataFragmentBinding
import com.heckfyxe.chatty.util.setAuthenticated
import com.soundcloud.android.crop.Crop
import kotlinx.android.synthetic.main.edit_user_data_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val EXTRA_NICKNAME = "com.heckfyxe.chatty.EXTRA_NICKNAME"
private const val EXTRA_PHOTO_FILE = "com.heckfyxe.chatty.EXTRA_PHOTO_FILE"
private const val EXTRA_CROPPED_PHOTO_FILE = "com.heckfyxe.chatty.EXTRA_CROPPED_PHOTO_FILE"

private const val RC_TAKE_PHOTO = 0
private const val RC_CROP_PHOTO = 1

class EditUserDataFragment : Fragment() {

    private val editUserDataViewModel: EditUserDataViewModel by viewModel()

    private var avatarImageFile: File? = null
    private var avatarCroppedImageFile: File? = null

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
                        Toast.makeText(context!!, R.string.error, LENGTH_SHORT).show()
                        return@Observer
                    }
                    photoFile?.also { file ->
                        val photoURI: Uri = file.toUri()
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

    private fun File.toUri(): Uri = FileProvider.getUriForFile(
        context!!,
        "com.heckfyxe.chatty.fileprovider",
        this
    )

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
                if (resultCode != RESULT_OK) {
                    Toast.makeText(context!!, R.string.error, LENGTH_SHORT).show()
                    return
                }

                avatarCroppedImageFile = createImageFile()
                Crop.of(avatarImageFile!!.toUri(), avatarCroppedImageFile!!.toUri())
                    .asSquare()
                    .start(context!!, this, RC_CROP_PHOTO)
            }
            RC_CROP_PHOTO -> {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(context!!, R.string.error, LENGTH_SHORT).show()
                    return
                }

                val bitmap = avatarCroppedImageFile!!.inputStream().use {
                    BitmapFactory.decodeStream(it)
                }
                avatarCroppedImageFile!!.outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
                }
                editUserDataViewModel.onPhotoTaken(avatarCroppedImageFile!!)
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState?.apply {
            val nickname = getCharSequence(EXTRA_NICKNAME)?.toString()
            avatarImageFile = getSerializable(EXTRA_PHOTO_FILE) as? File
            avatarCroppedImageFile = getSerializable(EXTRA_CROPPED_PHOTO_FILE) as? File

            if (nickname != null) {
                nicknameEditText?.setText(nickname)
                editUserDataViewModel.checkNickname(nickname)
            }
            editUserDataViewModel.onPhotoTaken(avatarCroppedImageFile ?: return)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putCharSequence(EXTRA_NICKNAME, nicknameEditText?.text)
            putSerializable(EXTRA_PHOTO_FILE, avatarImageFile)
            putSerializable(EXTRA_CROPPED_PHOTO_FILE, avatarCroppedImageFile)
        }
    }
}