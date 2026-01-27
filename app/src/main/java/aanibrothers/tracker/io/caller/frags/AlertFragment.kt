package aanibrothers.tracker.io.caller.frags

import aanibrothers.tracker.io.R
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coder.apps.space.library.base.BaseFragment
import aanibrothers.tracker.io.caller.alert.Alert
import aanibrothers.tracker.io.caller.alert.AlertAdapter
import aanibrothers.tracker.io.caller.alert.AlertReceiver
import aanibrothers.tracker.io.caller.alert.AppDatabase
import aanibrothers.tracker.io.databinding.FragmentAlertBinding
import coder.apps.space.library.extension.beGone
import coder.apps.space.library.extension.beVisible
import kotlinx.coroutines.launch
import java.util.Calendar

class AlertFragment : BaseFragment<FragmentAlertBinding>(FragmentAlertBinding::inflate) {

    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private var alertAdapter: AlertAdapter? = null

    override fun FragmentAlertBinding.viewCreated() {
        initAdapter()
    }

    fun FragmentAlertBinding.initAdapter() {
        activity?.apply context@{
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@context, RecyclerView.VERTICAL, false)
                alertAdapter = AlertAdapter { alert ->
                    deleteReminder(alert)
                    refreshList()
                }
                adapter = alertAdapter
                refreshList()
            }
        }
    }

    private fun deleteReminder(reminder: Alert) {
        lifecycleScope.launch {
            cancelScheduledNotification(reminder.requestCode)
            db.alertDao().deleteById(reminder.id)
        }
    }


    private fun FragmentAlertBinding.refreshList() {
        lifecycleScope.launch {
            val alerts = db.alertDao().getUpcoming(System.currentTimeMillis())
            alertAdapter?.setData(alerts)
            if (alerts.isEmpty()) {
                layoutNoData.beVisible()
            } else layoutNoData.beGone()
        }
    }

    override fun FragmentAlertBinding.initListeners() {
        activity?.apply context@{
            picker.setMinDate(Calendar.getInstance().time)

            buttonSaveReminder.setOnClickListener {
                Log.e(TAG, "initListeners: ${picker.date.time}")
                val selectedMillis = picker.date.time
                if (editReminder.text.toString().isEmpty()) {
                    Toast.makeText(activity, getString(R.string.please_add_reminder_title), Toast.LENGTH_SHORT).show()
                } else {
                    saveAlert(selectedMillis)
                }
            }

            buttonAdd.setOnClickListener {
                layoutPicker.beVisible()
            }

            buttonClose.setOnClickListener {
                layoutPicker.beGone()
                refreshList()
            }
        }
    }

    private fun FragmentAlertBinding.saveAlert(timeInMillis: Long) {
        val title = editReminder.text.toString().ifEmpty { "Alert" }
        val requestCode = System.currentTimeMillis().toInt()
        if (timeInMillis > System.currentTimeMillis()) {
            lifecycleScope.launch {
                db.alertDao().insert(Alert(title = title, timeInMillis = timeInMillis, requestCode = requestCode))
                scheduleNotification(timeInMillis, title, requestCode)
            }
            layoutPicker.beGone()
        } else {
            Toast.makeText(activity, "Select a future time", Toast.LENGTH_SHORT).show()
        }
        refreshList()
    }

    private fun scheduleNotification(time: Long, title: String, requestCode: Int) {
        val intent = Intent(requireContext(), AlertReceiver::class.java).apply {
            putExtra("title", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
    }

    private fun cancelScheduledNotification(requestCode: Int) {
        val intent = Intent(requireContext(), AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }


    override fun FragmentAlertBinding.initView() {
    }

    override fun create() {
    }

    companion object {
        private const val TAG = "AlertFragment"
        fun newInstance() = AlertFragment().apply {
            arguments = Bundle().apply {}
        }
    }
}
