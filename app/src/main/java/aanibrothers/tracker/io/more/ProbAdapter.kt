package aanibrothers.tracker.io.more

import aanibrothers.tracker.io.databinding.ListItemSurveyOptionBinding
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class ProbAdapter(private val context: Context, private val messages: Array<String>,private val onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<ProbAdapter.ViewHolder>() {

    private var selectedItemPosition = RecyclerView.NO_POSITION

    interface OnItemClickListener {
        fun onItemClicked(reason: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemSurveyOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, i: Int) {
        viewHolder.binding.apply {
            tvOptionText.text = messages[i]
            rbOptionSelect.isChecked = selectedItemPosition == i
        }
        viewHolder.itemView.setOnClickListener {
            setSelectedItemPosition(i)
            onItemClickListener.onItemClicked(messages[i])
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    private fun setSelectedItemPosition(position: Int) {
        val previousSelectedPosition: Int = selectedItemPosition
        selectedItemPosition = position
        notifyItemChanged(previousSelectedPosition)
        notifyItemChanged(selectedItemPosition)
    }

    class ViewHolder(val binding: ListItemSurveyOptionBinding) : RecyclerView.ViewHolder(binding.root)
}