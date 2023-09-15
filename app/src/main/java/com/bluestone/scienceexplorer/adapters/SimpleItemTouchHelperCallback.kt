package com.bluestone.scienceexplorer.adapters

import android.graphics.Canvas
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scienceexplorer.constants.SWIPE_DIRECTION_LEFT
import com.bluestone.scienceexplorer.constants.SWIPE_DIRECTION_RIGHT
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.database.Cache
import com.bluestone.scienceexplorer.interfaces.ItemTouchHelperAdapter
import kotlinx.coroutines.channels.SendChannel
import kotlin.math.abs


class SimpleItemTouchHelperCallback(
    val activity: AppCompatActivity,
    private val adapter: ItemTouchHelperAdapter,
    private val sync: SendChannel<Boolean>? = null
) : ItemTouchHelper.Callback() {
    override fun isLongPressDragEnabled() = true
    override fun isItemViewSwipeEnabled() = true
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags =
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val width = viewHolder.itemView.width.toFloat()
            val alpha = 1.0f - abs(dX) / width
            viewHolder.itemView.alpha = alpha
            viewHolder.itemView.translationX = dX
        } else {
            super.onChildDraw(
                c, recyclerView, viewHolder, dX, dY,
                actionState, isCurrentlyActive
            )
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.onItemMove(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        Log.d(TAG, "onSwiped -> direction: $direction ")
        when (direction) {
            SWIPE_DIRECTION_LEFT,
            SWIPE_DIRECTION_RIGHT -> {
                sync?.trySend(true)
                adapter.onItemDismiss(viewHolder)
            }
        }
    }
}