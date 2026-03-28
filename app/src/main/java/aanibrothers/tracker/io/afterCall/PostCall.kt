package aanibrothers.tracker.io.afterCall

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import androidx.core.content.ContextCompat
import aanibrothers.tracker.io.afterCall.service.MyJobService
import android.Manifest

class PostCall private constructor() {
    fun startPostCall(context: Context) {
        val checkCallPhonePermission = hasPermission(context)
        Log.e("PostCall", "startPostCall call  checkPermission--> " + checkCallPhonePermission)
        if (hasPermission(context)) {
            val systemService = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)

            val jobScheduler = systemService as JobScheduler
            val persistableBundle = PersistableBundle()
            persistableBundle.putInt("job_scheduler_source", 1)
            val builder =
                JobInfo.Builder(666, ComponentName(context, MyJobService::class.java))
                    .setExtras(persistableBundle)
                    .setMinimumLatency(0L)
            if (Build.VERSION.SDK_INT >= 26) {
                builder.setRequiresBatteryNotLow(true)
            }
            if (jobScheduler.allPendingJobs.size > 50) {
                val it = jobScheduler.allPendingJobs.iterator()
                while (it.hasNext()) {
                    Log.e("PostCall", "job = " + it.next())
                }
                jobScheduler.cancelAll()
            }
            if (Build.VERSION.SDK_INT >= 24) {
                if (jobScheduler.getPendingJob(666) != null) {
                    jobScheduler.cancel(666)
                }
            }
            try {
                jobScheduler.schedule(builder.build())
            } catch (e: IllegalArgumentException) {
                Log.e("PostCall", "Job schedule failed", e)
            }
            Log.e("PostCall", "Scheduled job successfully!")
        }
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == 0
    }

    companion object {
        @JvmField
        val INSTANCE: PostCall = PostCall()
    }
}
