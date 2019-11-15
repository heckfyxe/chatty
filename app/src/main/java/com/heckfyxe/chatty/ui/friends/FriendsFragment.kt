package com.heckfyxe.chatty.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.heckfyxe.chatty.R
import com.heckfyxe.chatty.model.User
import com.heckfyxe.chatty.util.sendbird.saveOnDevice
import kotlinx.android.synthetic.main.friends_fragment.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class FriendsFragment : Fragment() {

    private val viewModel: FriendsViewModel by viewModel()

    private lateinit var friendsAdapter: FriendsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        friendsAdapter = FriendsAdapter { friend ->
            viewModel.createChannel(friend) {
                it.saveOnDevice()
                val direction =
                    FriendsFragmentDirections.actionFriendsFragmentToMessageFragment(
                        it.url,
                        with(friend) {
                            User(userId, nickname, profileUrl)
                        }
                    )
                findNavController().navigate(direction)
            }
        }

        showProgressBar()
        viewModel.loadFriends()
        connectViewModel()
    }

    private fun connectViewModel() {
        viewModel.friends.observe(this, Observer {
            if (it.isEmpty()) {
                showNoFriends()
            } else {
                hideAdditions()
                friendsAdapter.update(it)
            }
        })

        viewModel.errors.observe(this, Observer {
            Snackbar.make(friendsRecyclerView, R.string.connection_error, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.retry) {
                    viewModel.loadFriends()
                }.show()
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.friends_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        friendsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context!!)
            adapter = friendsAdapter
        }
    }

    private fun showProgressBar() {
        friendsProgressBar?.isVisible = true
        noFriendsTextView?.isGone = true
    }

    private fun showNoFriends() {
        friendsProgressBar?.isGone = true
        noFriendsTextView?.isVisible = true
    }

    private fun hideAdditions() {
        friendsProgressBar?.isGone = true
        noFriendsTextView?.isGone = true
    }
}
