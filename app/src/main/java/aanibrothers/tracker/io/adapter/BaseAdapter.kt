package aanibrothers.tracker.io.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

abstract class BaseXAdapter<T, VB : ViewBinding>(
    private val bindingFactory: (LayoutInflater, ViewGroup, Boolean, viewType: Int) -> VB,
    val items: MutableList<T> = mutableListOf(),
) : RecyclerView.Adapter<BaseXAdapter<T, VB>.BaseViewHolder>() {

    abstract fun VB.bind(item: T, position: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val binding = bindingFactory(LayoutInflater.from(parent.context), parent, false, viewType)
        return BaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.binding.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newItems: MutableList<T>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun addItems(newMessages: MutableList<T>) {
        val startPos = items.size
        items.addAll(newMessages)
        notifyItemRangeInserted(startPos, newMessages.size)
    }

    fun addItem(item: T) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    /** Adds an item at the top (position 0) and scrolls UI properly */
    fun addItemAtTop(item: T) {
        items.add(0, item)
        notifyItemInserted(0)
    }

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class BaseViewHolder(val binding: VB) : RecyclerView.ViewHolder(binding.root)
}
