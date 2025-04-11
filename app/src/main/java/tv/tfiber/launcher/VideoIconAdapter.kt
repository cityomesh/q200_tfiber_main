package tv.tfiber.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class VideoIconAdapter(  // Corrected constructor
    private val icons: List<IconItem>,
    private val onItemClick: (IconItem) -> Unit
) :
    RecyclerView.Adapter<VideoIconAdapter.IconViewHolder>() {

    class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.iconImageView)

        fun bind(iconItem: IconItem, itemClickListener: (IconItem) -> Unit) {
            imageView.setImageResource(iconItem.iconResId)
            itemView.setOnClickListener {
                itemClickListener(iconItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_icon, parent, false) // Inflate the new layout
        return IconViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val iconItem = icons[position]
        holder.bind(iconItem, onItemClick)
          // Or another background
    }

    override fun getItemCount(): Int {
        return icons.size
    }
}