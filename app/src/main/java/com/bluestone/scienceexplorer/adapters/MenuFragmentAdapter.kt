package com.bluestone.scienceexplorer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scenceexplorer.R
import com.bluestone.scienceexplorer.database.Cache
import com.bluestone.scienceexplorer.dataclasses.SelectedChannel
import com.bluestone.scienceexplorer.interfaces.ItemTouchHelperAdapter
import com.squareup.picasso.Picasso
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel


class MenuFragmentAdapter(private val recyclerView: RecyclerView) : ItemTouchHelperAdapter,
    RecyclerView.Adapter<MenuFragmentAdapter.ViewHolder>() {
    private val dataSet = mutableListOf<SelectedChannel>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var channelTitle: TextView = itemView.findViewById(R.id.txt_channel_titile)
        var channelImage: ImageView = itemView.findViewById(R.id.img_channel_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_menu_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.channelTitle.text = dataSet[position].channelTitle
        holder.channelImage.setOnClickListener {
            selectedChannel_.trySend(dataSet[position])
        }
        dataSet[position].channelItem?.let { channelItem ->
            val width = channelItem.width
            val height = channelItem.height
            Picasso.get()
                .load(channelItem.url_medium)
                .resize(width, height)
                .centerCrop()
                .error(androidx.appcompat.R.drawable.abc_item_background_holo_dark)
                .into(holder.channelImage)
        }
    }

    fun clear() {
        dataSet.clear()
        notifyDataSetChanged()
    }

    fun update(item: SelectedChannel) {
        dataSet.add(item)
        if (dataSet.isNotEmpty()) {
            notifyItemInserted(dataSet.size - 1)
        } else {
            notifyItemInserted(0)
        }
    }

    override fun getItemCount() = dataSet.size

    companion object {
        private val selectedChannel_ = Channel<SelectedChannel>(Channel.CONFLATED)
        val selectedChannel: ReceiveChannel<SelectedChannel>
            get() = selectedChannel_

    }

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        //TODO:Disabling this code for now. TO use this functionality
        // need to create a list of [title, index] and update dataSet with new idexeces
        // As it is, refreshing the vew restores the indexece to the state prior to the move.
//        if (fromPosition < toPosition) {
//            for (i in fromPosition until toPosition) {
//                Collections.swap(dataSet, i, i + 1)
//            }
//        } else {
//            for (i in fromPosition downTo toPosition + 1) {
//                Collections.swap(dataSet, i, i - 1)
//            }
//        }
//        notifyItemMoved(fromPosition, toPosition)
//        notifyItemChanged(fromPosition)
//        notifyItemChanged(toPosition)
        return true
    }

    override fun onItemDismiss(viewHolder: RecyclerView.ViewHolder) {
        val selectedItem = dataSet[viewHolder.adapterPosition]
        val clientID = Cache.generateOrFetchClientID()
        Cache.deleteChannel(clientID, selectedItem.channelID)
        dataSet.removeAt(viewHolder.adapterPosition)
        notifyItemRemoved(viewHolder.adapterPosition)
    }
}