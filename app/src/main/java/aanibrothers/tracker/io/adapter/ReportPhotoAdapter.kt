package aanibrothers.tracker.io.adapter

import aanibrothers.tracker.io.databinding.ItemReportPhotoBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

/**
 * Photo grid for the report screen, with multi-select.
 *
 * Selection is keyed on the file's absolute path rather than its position, so
 * it survives the list being re-sorted or reloaded.
 */
class ReportPhotoAdapter(
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<ReportPhotoAdapter.PhotoViewHolder>() {

    private val photos = mutableListOf<File>()
    private val selectedPaths = linkedSetOf<String>()

    val selectedCount: Int get() = selectedPaths.size

    /** Selected files, in the order the user picked them. */
    fun selectedPhotos(): List<File> {
        return selectedPaths.mapNotNull { path -> photos.firstOrNull { it.absolutePath == path } }
    }

    fun submit(newPhotos: List<File>) {
        photos.clear()
        photos.addAll(newPhotos)
        selectedPaths.retainAll(newPhotos.map { it.absolutePath }.toSet())
        notifyDataSetChanged()
        onSelectionChanged(selectedPaths.size)
    }

    fun selectAll() {
        selectedPaths.clear()
        selectedPaths.addAll(photos.map { it.absolutePath })
        notifyDataSetChanged()
        onSelectionChanged(selectedPaths.size)
    }

    fun clearSelection() {
        selectedPaths.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedPaths.size)
    }

    fun isAllSelected(): Boolean = photos.isNotEmpty() && selectedPaths.size == photos.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            ItemReportPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]
        val isSelected = selectedPaths.contains(photo.absolutePath)

        holder.binding.apply {
            Glide.with(imagePhoto).load(photo).centerCrop().into(imagePhoto)
            viewSelectionScrim.visibility = if (isSelected) View.VISIBLE else View.GONE
            imageCheck.visibility = if (isSelected) View.VISIBLE else View.GONE

            root.setOnClickListener {
                if (!selectedPaths.add(photo.absolutePath)) {
                    selectedPaths.remove(photo.absolutePath)
                }
                // Look the row up by file rather than trusting a captured
                // position, which goes stale when the list reloads.
                notifyItemChanged(photos.indexOf(photo))
                onSelectionChanged(selectedPaths.size)
            }
        }
    }

    override fun getItemCount(): Int = photos.size

    class PhotoViewHolder(val binding: ItemReportPhotoBinding) :
        RecyclerView.ViewHolder(binding.root)
}
