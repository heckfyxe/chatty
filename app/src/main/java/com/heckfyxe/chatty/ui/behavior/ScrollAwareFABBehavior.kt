package com.heckfyxe.chatty.ui.behavior

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ScrollAwareFABBehavior(
    context: Context,
    attrs: AttributeSet
)// This is mandatory if we're assigning the behavior straight from XML
    : FloatingActionButton.Behavior() {
//
//    override fun onStartNestedScroll(
//        coordinatorLayout: CoordinatorLayout,
//        child: FloatingActionButton,
//        directTargetChild: View,
//        target: View,
//        nestedScrollAxes: Int
//    ): Boolean {
//        // Ensure we react to vertical scrolling
//        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(
//            coordinatorLayout,
//            child,
//            directTargetChild,
//            target,
//            nestedScrollAxes
//        )
//    }
//
//    override fun onNestedScroll(
//        coordinatorLayout: CoordinatorLayout,
//        child: FloatingActionButton,
//        target: View,
//        dxConsumed: Int,
//        dyConsumed: Int,
//        dxUnconsumed: Int,
//        dyUnconsumed: Int
//    ) {
//        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
//        if (dyConsumed > 0 && child.visibility == View.VISIBLE) {
//            // RoomUser scrolled down and the FAB is currently visible -> hide the FAB
//            child.hide()
//        } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
//            // RoomUser scrolled up and the FAB is currently not visible -> show the FAB
//            child.show()
//        }
//    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(
            coordinatorLayout,
            child,
            directTargetChild,
            target,
            axes,
            type
        )
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        if (type == ViewCompat.TYPE_TOUCH) {
            if (dyConsumed > 0 && child.visibility == View.VISIBLE) {
                // RoomUser scrolled down and the FAB is currently visible -> hide the FAB
                child.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                    @SuppressLint("RestrictedApi")
                    override fun onHidden(fab: FloatingActionButton?) {
                        super.onHidden(fab)
                        fab?.visibility = View.INVISIBLE
                    }
                })
            } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
                // RoomUser scrolled up and the FAB is currently not visible -> show the FAB
                child.show()
            }
        }
    }
}