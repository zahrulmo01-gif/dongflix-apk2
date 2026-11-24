package com.dongflix

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class GridAdapter(
	private val context: Context,
	private val list: List<DonghuaModel>
) : BaseAdapter() {

	override fun getCount(): Int = list.size

	override fun getItem(position: Int): Any = list[position]

	override fun getItemId(position: Int): Long = position.toLong()

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_grid_anime, parent, false)

		val item = list[position]
		val imgCover = view.findViewById<ImageView>(R.id.img_cover)
		val textTitle = view.findViewById<TextView>(R.id.text_title)
		val textEpisode = view.findViewById<TextView>(R.id.text_episode)

		textTitle.text = item.title
		textEpisode.text = item.episode

		Glide.with(context)
			.load(item.cover)
			.placeholder(android.R.color.darker_gray)
			.into(imgCover)

		return view
	}
}
