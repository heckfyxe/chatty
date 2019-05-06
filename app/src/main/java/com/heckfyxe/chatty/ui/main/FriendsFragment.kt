package com.heckfyxe.chatty.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.heckfyxe.chatty.R
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
                it.saveOnDevice(activity!!.applicationContext)
                val direction = FriendsFragmentDirections.actionFriendsFragmentToMessageFragment(it.url)
                findNavController().navigate(direction)
            }
        }

        viewModel.loadFriends()
        connectViewModel()
    }

    private fun connectViewModel() {
        viewModel.friends.observe(this, Observer {
            friendsAdapter.update(it)
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
}
