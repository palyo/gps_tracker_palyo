package aanibrothers.tracker.io.adapter

import aanibrothers.tracker.io.databinding.ItemPdfFileBinding
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Previously generated PDF reports. */
class PdfFileAdapter(
    private val onOpen: (File) -> Unit,
    private val onShare: (File) -> Unit
) : RecyclerView.Adapter<PdfFileAdapter.PdfViewHolder>() {

    private val files = mutableListOf<File>()

    fun submit(newFiles: List<File>) {
        files.clear()
        files.addAll(newFiles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        return PdfViewHolder(
            ItemPdfFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val file = files[position]
        holder.binding.apply {
            val context = root.context
            textPdfName.text = file.name
            val date = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(Date(file.lastModified()))
            textPdfMeta.text = "$date  ·  ${Formatter.formatShortFileSize(context, file.length())}"

            root.setOnClickListener { onOpen(file) }
            actionSharePdf.setOnClickListener { onShare(file) }
        }
    }

    override fun getItemCount(): Int = files.size

    class PdfViewHolder(val binding: ItemPdfFileBinding) : RecyclerView.ViewHolder(binding.root)
}
