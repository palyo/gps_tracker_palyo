package aanibrothers.tracker.io.adapter

import aanibrothers.tracker.io.databinding.ItemReportImageBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

/** Images added to the report, each removable. */
class ReportImageAdapter(
    private val onRemove: (File) -> Unit,
    private val onClick: (File) -> Unit
) : RecyclerView.Adapter<ReportImageAdapter.ImageViewHolder>() {

    private val images = mutableListOf<File>()

    fun submit(newImages: List<File>) {
        images.clear()
        images.addAll(newImages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ItemReportImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]
        holder.binding.apply {
            Glide.with(imagePhoto).load(image).centerCrop().into(imagePhoto)
            actionRemove.setOnClickListener { onRemove(image) }
            imagePhoto.setOnClickListener { onClick(image) }
        }
    }

    override fun getItemCount(): Int = images.size

    class ImageViewHolder(val binding: ItemReportImageBinding) :
        RecyclerView.ViewHolder(binding.root)
}
