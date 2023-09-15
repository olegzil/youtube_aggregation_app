package com.bluestone.scienceexplorer.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scenceexplorer.R
import com.bluestone.scienceexplorer.constants.INTENT_KEY_VIDEO_PLAYLIST
import com.bluestone.scienceexplorer.constants.SHARED_LINK_URL
import com.bluestone.scienceexplorer.database.Cache
import com.bluestone.scienceexplorer.dataclasses.PlaylistDescriptor
import com.bluestone.scienceexplorer.dataclasses.SharedLink
import com.bluestone.scienceexplorer.interfaces.ItemTouchHelperAdapter
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select


class RandomLinkAdapter(private val recyclerView: RecyclerView) : ItemTouchHelperAdapter,
    RecyclerView.Adapter<RandomLinkAdapter.ViewHolder>() {
    private val dataSet = mutableListOf<SharedLink>()
    private val networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var videoThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var videoTitle: TextView = itemView.findViewById(R.id.txt_title)
        var videoDate: TextView = itemView.findViewById(R.id.txt_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.video_thumb_nail, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = dataSet.size

    fun update(item: SharedLink) {
        dataSet.add(item)
        if (dataSet.isNotEmpty()) {
            notifyItemInserted(dataSet.size - 1)
        } else {
            notifyItemInserted(0)
        }
    }

    fun clear() {
        if (dataSet.isNotEmpty()) {
            dataSet.clear()
            notifyDataSetChanged()
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.videoTitle.text = dataSet[position].name
        holder.videoDate.visibility = View.GONE
        holder.videoThumbnail.clipToOutline = true
        holder.videoThumbnail.setOnClickListener {
            selectedVideo_.trySend(Pair(position, dataSet[position]))
        }
        if (dataSet[position].type == INTENT_KEY_VIDEO_PLAYLIST) {
            val asyncResult = networkScope.async {
                loadImageFromWebAsync(dataSet[position].url)
            }
            uiScope.launch {
                val result = asyncResult.await()
                val width = result.width
                val height = result.height
                Picasso.get()
                    .load(result.imageURL)
                    .resize(width, height)
                    .centerCrop(Gravity.CENTER)
                    .error(androidx.appcompat.R.drawable.abc_item_background_holo_dark)
                    .into(holder.videoThumbnail)
            }
        } else {
            val url = SHARED_LINK_URL.format(dataSet[position].url)
            val displayMetrics = holder.videoThumbnail.resources.displayMetrics
            val width = (displayMetrics.widthPixels / displayMetrics.density).toInt()
            val height = (displayMetrics.heightPixels / displayMetrics.density).toInt()
            Picasso.get()
                .load(url)
                .resize(width, height)
                .centerCrop(Gravity.CENTER)
                .error(androidx.appcompat.R.drawable.abc_item_background_holo_dark)
                .into(holder.videoThumbnail)
        }
    }

    private suspend fun loadImageFromWebAsync(url: String): PlaylistDescriptor {
        var initialValue: PlaylistDescriptor = PlaylistDescriptor()
        val retVal = Cache.fetchPlaylistPage(url)
        while (select {
                retVal.onSuccess.onReceive {
                    initialValue =
                        it.fold(initialValue) { acc, e -> if (e.height > acc.height) e else acc }
                    true
                }
                retVal.onComplete.onReceive {

                    false
                }
            }) {/*empty body*/
        }
        return initialValue
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
        val adapter = (recyclerView.adapter as RandomLinkAdapter)
        val selectedItem = adapter.dataSet[viewHolder.adapterPosition]
        Cache.deleteSharedLink(selectedItem.url)
        adapter.dataSet.removeAt(viewHolder.adapterPosition)
        adapter.notifyItemRemoved(viewHolder.adapterPosition)
    }

    companion object {
        private val selectedVideo_ = Channel<Pair<Int, SharedLink>>(Channel.CONFLATED)
        val selectedVideo: ReceiveChannel<Pair<Int, SharedLink>>
            get() = selectedVideo_

    }

}