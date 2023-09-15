package com.bluestone.scienceexplorer.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bluestone.scenceexplorer.R
import com.bluestone.scienceexplorer.constants.TAG
import com.bluestone.scienceexplorer.dataclasses.VideoThumbnailItem
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.*

@SuppressLint("ClickableViewAccessibility")
class VideoThumbnailAdapter :
    RecyclerView.Adapter<VideoThumbnailAdapter.ViewHolder>() {
    private val dataSet = mutableListOf<VideoThumbnailItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.video_thumb_nail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.videoTitle.text = dataSet[position].title
        holder.videoDate.text = dataSet[position].date
        holder.videoThumbnail.clipToOutline = true
        holder.videoThumbnail.setOnClickListener {
            selectedVideo_.trySend(Pair(position, dataSet[position]))
        }
        val width = dataSet[position].width*2
        val height = dataSet[position].height*2
        Picasso.get()
            .load(dataSet[position].urlMedium)
            .resize(width, height)
            .centerCrop()
            .error(androidx.appcompat.R.drawable.abc_item_background_holo_dark)
            .into(holder.videoThumbnail)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var videoThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        var videoTitle = itemView.findViewById<TextView>(R.id.txt_title)
        var videoDate = itemView.findViewById<TextView>(R.id.txt_date)
    }

    fun clear() {
        if (dataSet.isNotEmpty()) {
            dataSet.clear()
            notifyDataSetChanged()
        }
    }

    fun update(item: VideoThumbnailItem) {
        dataSet.add(item)
        if (dataSet.isNotEmpty()) {
            notifyItemInserted(dataSet.size - 1)
        } else {
            notifyItemInserted(0)
        }
    }

    companion object {
        private val selectedVideo_ = Channel<Pair<Int, VideoThumbnailItem>>(Channel.CONFLATED)
        val selectedVideo: ReceiveChannel<Pair<Int, VideoThumbnailItem>>
            get() = selectedVideo_

    }
}