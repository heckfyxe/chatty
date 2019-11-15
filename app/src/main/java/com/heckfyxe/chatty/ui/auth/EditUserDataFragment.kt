package com.heckfyxe.chatty.ui.auth

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import coil.ImageLoader
import coil.api.load
import coil.decode.GifDecoder
import coil.transform.CircleCropTransformation
import com.google.android.material.snackbar.Snackbar
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.util.setAuthenticated
import kotlinx.android.synthetic.main.edit_user_data_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditUserDataFragment : Fragment() {

    private val model: EditUserDataViewModel by viewModel()

    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var imageLoader: ImageLoader

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageLoader = ImageLoader(context) {
            componentRegistry {
                add(GifDecoder())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.init()
        observeViewModel()
        model.connectUser()
    }

    private fun observeViewModel() {
        model.checkedNicknameLiveData.observe(this, Observer {
            if (it.nickname == nicknameEditText?.text?.toString()) {
                hideNicknameChecking()
                if (it.allowed) {
                    nicknameInputLayout?.isHelperTextEnabled = true
                    nicknameInputLayout?.helperText = getString(R.string.allowed)
                    val greenColor = ContextCompat.getColor(context!!, R.color.green)
                    nicknameInputLayout?.setHelperTextColor(ColorStateList.valueOf(greenColor))
                    nicknameOkButton?.isEnabled = true
                } else {
                    nicknameInputLayout?.isErrorEnabled = true
                    nicknameInputLayout?.error = resources.getString(R.string.the_nickname_is_already_taken)
                }
            }
        })

        model.currentUser.observe(this, Observer {
            startAvatarLoading(it.profileUrl)
            nicknameEditText?.setText(it.nickname)
            nicknameEditText?.isEnabled = true
        })

        model.errors.observe(this, Observer {
            when (it.type) {
                EditUserDataViewModel.ErrorType.CONNECT_USER -> showConnectUserError()

                EditUserDataViewModel.ErrorType.UPDATE_USER_DATA -> {
                    nicknameOkButton?.revertAnimation {
                    }
                    nicknameOkButton?.isEnabled = true
                    Snackbar.make(editUserDataFragmentRoot, R.string.data_updating_error, Snackbar.LENGTH_SHORT).show()
                }

                EditUserDataViewModel.ErrorType.CHECK_NICKNAME -> {
                    if (it.extra?.get("nickname").toString() == nicknameEditText?.text.toString()) {
                        showNicknameCheckingError()
                    }
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.edit_user_data_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startAvatarLoadingAnimation()

        nicknameEditText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                nicknameOkButton?.isEnabled = false
                hideNicknameChecking()
                nicknameInputLayout?.apply {
                    isErrorEnabled = false
                    isHelperTextEnabled = false
                }

                if (s.isNullOrBlank()) return

                showNicknameCheckingProgress()
                scope.launch { model.checkingNicknameChannel.send(s.toString()) }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        nicknameEditText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && nicknameOkButton?.isEnabled == true) {
                updateUserData()
                return@setOnEditorActionListener true
            }
            false
        }

        nicknameOkButton?.apply {
            saveInitialState()
            setOnClickListener {
                updateUserData()
            }
        }
    }

    private fun updateUserData() {
        if (!nicknameEditText?.text.isNullOrEmpty()) {
            nicknameOkButton?.startAnimation()
            model.updateUserData(nicknameEditText.text.toString()) {
                setAuthenticated()
                findNavController().navigate(R.id.action_editUserDataFragment_to_contactFragment)
            }
        }
    }

    private fun startAvatarLoadingAnimation() {
        circleImageView?.isVisible = true
    }

    private fun startAvatarLoading(avatarUrl: String) {
        circleImageView?.load(avatarUrl, imageLoader) {
            placeholder(R.drawable.spinner)
            transformations(CircleCropTransformation())
        }
    }

    private fun showNicknameCheckingProgress() {
        nicknameCheckingProgressBar?.isVisible = true
        nicknameCheckingErrorTextView?.isGone = true
    }

    private fun hideNicknameChecking() {
        nicknameCheckingErrorTextView?.isGone = true
        nicknameCheckingProgressBar?.isVisible = false
    }

    private fun avatarLoadingFailed() {
        circleImageView?.isVisible = false
    }

    private fun showConnectUserError() {
        avatarLoadingFailed()
        Snackbar.make(editUserDataFragmentRoot, R.string.connection_error, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.retry) {
                startAvatarLoadingAnimation()
                model.connectUser()
            }.show()
    }

    private fun showNicknameCheckingError() {
        nicknameCheckingErrorTextView?.isVisible = true
        nicknameCheckingProgressBar?.isVisible = false
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState?.apply {
            nicknameEditText?.setText(getCharSequence(EXTRA_NICKNAME))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.apply {
            putCharSequence(EXTRA_NICKNAME, nicknameEditText?.text)
        }
    }

    companion object {
        private const val EXTRA_NICKNAME = "com.heckfyxe.chatty.EXTRA_NICKNAME"
    }
}
