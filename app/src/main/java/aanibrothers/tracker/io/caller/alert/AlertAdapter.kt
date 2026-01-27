package aanibrothers.tracker.io.caller.alert

import aanibrothers.tracker.io.databinding.LayoutRowItemReminderBinding
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertAdapter(
    private val onDelete: (Alert) -> Unit
) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    private val alerts = mutableListOf<Alert>()

    fun setData(list: List<Alert>) {
        alerts.clear()
        alerts.addAll(list)
        notifyDataSetChanged()
    }

    inner class AlertViewHolder(val binding: LayoutRowItemReminderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        return AlertViewHolder(LayoutRowItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val item = alerts[position]
        holder.binding.apply {
            title.text = item.title

            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            time.text = sdf.format(Date(item.timeInMillis))
            delete.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount() = alerts.size
}
