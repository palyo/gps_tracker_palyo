package aanibrothers.tracker.io.afterCall.ui.adapter

import aanibrothers.tracker.io.R
import aanibrothers.tracker.io.databinding.PostItemQuickReplyBinding
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class QuickReplyAdapter(
    private val context: Context,
    private val quickReplyList: ArrayList<QuickReplyModel>,
    private val onQuickReplyClick: (Int) -> Unit
) : RecyclerView.Adapter<QuickReplyAdapter.ViewHolderHoliday>() {

    var selectPos = -1

    override fun getItemCount(): Int = quickReplyList.size

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderHoliday {
        val binding = PostItemQuickReplyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolderHoliday(binding)
    }

    override fun onBindViewHolder(holder: ViewHolderHoliday, position: Int) {
        val item = quickReplyList[position]
        val binding = holder.binding

        binding.name.text = item.name

        val isSelected = selectPos == position

        binding.name.setTextColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.colorAccent else R.color.colorText
            )
        )

        binding.radio.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                if (isSelected) R.drawable.post_checkbox_selected
                else R.drawable.post_checkbox_unselected
            )
        )

        binding.icSend.visibility = if (isSelected) View.VISIBLE else View.GONE

        binding.viewLine.visibility =
            if (position == quickReplyList.lastIndex) View.INVISIBLE else View.VISIBLE

        holder.itemView.setOnClickListener {
            val oldPos = selectPos

            if (oldPos == position) {
                onQuickReplyClick(position)
            } else {
                selectPos = position
                notifyItemChanged(position)

                if (oldPos != -1) {
                    notifyItemChanged(oldPos)
                }
            }
        }
    }

    fun selectionClear() {
        val oldPos = selectPos
        selectPos = -1
        if (oldPos != -1) notifyItemChanged(oldPos)
    }

    class ViewHolderHoliday(val binding: PostItemQuickReplyBinding) :
        RecyclerView.ViewHolder(binding.root)
}