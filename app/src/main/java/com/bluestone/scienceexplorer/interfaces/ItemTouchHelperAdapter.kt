package com.bluestone.scienceexplorer.interfaces

import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scienceexplorer.dataclasses.SelectedChannel

interface ItemTouchHelperAdapter {
    fun onItemMove(fromPosition: Int, toPosition: Int) : Boolean
    fun onItemDismiss(viewHolder: RecyclerView.ViewHolder)
}