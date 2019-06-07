package com.heckfyxe.chatty.ui.message

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RecyclerScrollMoreListener(
    private val layoutManager: LinearLayoutManager,
    private val loadMoreListener: OnLoadMoreListener?
) : RecyclerView.OnScrollListener() {
    private var currentPage = 0
    private var previousTotalItemCount = 0
    private var loading = true

    private fun getLastVisibleItem(lastVisibleItemPositions: IntArray): Int {
        var maxSize = 0
        for (i in lastVisibleItemPositions.indices) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i]
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i]
            }
        }
        return maxSize
    }

    override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        if (loadMoreListener != null) {
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
            val totalItemCount = layoutManager.itemCount

            if (totalItemCount < previousTotalItemCount) {
                this.currentPage = 0
                this.previousTotalItemCount = totalItemCount
                if (totalItemCount == 0) {
                    this.loading = true
                }
            }

            if (loading && totalItemCount > previousTotalItemCount) {
                loading = false
                previousTotalItemCount = totalItemCount
            }

            val visibleThreshold = 5
            if (!loading && lastVisibleItemPosition + visibleThreshold > totalItemCount) {
                currentPage++
                loadMoreListener.onLoadMore(loadMoreListener.messagesCount, totalItemCount)
                loading = true
            }
        }
    }

    interface OnLoadMoreListener {

        val messagesCount: Int
        fun onLoadMore(page: Int, total: Int)
    }
}
