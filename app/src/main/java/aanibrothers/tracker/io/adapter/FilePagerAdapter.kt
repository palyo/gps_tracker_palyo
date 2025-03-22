package aanibrothers.tracker.io.adapter

import aanibrothers.tracker.io.databinding.*
import aanibrothers.tracker.io.ui.*
import android.app.*
import android.view.*
import androidx.recyclerview.widget.*
import coder.apps.space.library.extension.*
import com.bumptech.glide.*
import com.bumptech.glide.load.resource.drawable.*
import com.bumptech.glide.request.*
import java.io.*

class FilePagerAdapter(val context: Activity?) : RecyclerView.Adapter<FilePagerAdapter.DataViewHolder>() {
    var items: MutableList<File>? = mutableListOf()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        return DataViewHolder(FragmentViewerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    fun addAll(original: MutableList<File>?) {
        original?.let { items?.addAll(it) }
        notifyItemRangeChanged(0, items?.size ?: 0)
    }

    fun removePage(index: Int) {
        items?.let {
            if (index in it.indices) {
                it.removeAt(index)
                notifyDataSetChanged()
            } else {
                "removePage".log("Invalid index: $index, size: ${it.size}")
            }
        } ?: "removePage".log("items is null")
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    override fun onViewRecycled(holder: DataViewHolder) {
        if (context?.isDestroyed?.not() == true && context.isFinishing.not()) Glide.with(holder.binding.imageMedia.context).clear(holder.binding.imageMedia)

        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val media = items?.get(position)
        context?.apply context@{
            holder.binding.apply {
                if (!this@context.isDestroyed && !this@context.isFinishing) {
                    media?.let { media ->
                        imageMedia.apply {
                            Glide.with(this@context.applicationContext).load(media.path).transition(DrawableTransitionOptions.withCrossFade()).apply(
                                RequestOptions().dontTransform().dontAnimate().skipMemoryCache(false)
                            ).into(this)
                        }
                    }

                    imageMedia.setOnClickListener {
                        (this@context as ViewCollectionActivity).fragmentClicked()
                    }
                }
            }
        }
    }

    class DataViewHolder(var binding: FragmentViewerBinding) : RecyclerView.ViewHolder(binding.root)
}