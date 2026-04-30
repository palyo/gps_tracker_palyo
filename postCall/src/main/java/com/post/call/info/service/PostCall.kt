package com.post.call.info.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.util.Log
import androidx.core.content.ContextCompat

class PostCall private constructor() {
    fun startPostCall(context: Context) {
        val checkCallPhonePermission = checkCallPhonePermission(context)
        Log.e("PostCall", "startPostCall call  checkPermission--> $checkCallPhonePermission")
        if (checkCallPhonePermission(context)) {
            val systemService = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)

            val jobScheduler = systemService as JobScheduler
            val persistableBundle = PersistableBundle()
            persistableBundle.putInt("job_scheduler_source", 1)
            val minimumLatency = JobInfo.Builder(666,
                ComponentName(context, MyJobService::class.java)
            ).setExtras(persistableBundle).setPersisted(true).setMinimumLatency(0L)
            if (Build.VERSION.SDK_INT >= 26) {
                minimumLatency.setRequiresBatteryNotLow(true)
            }
            if (jobScheduler.allPendingJobs.size > 50) {
                val it = jobScheduler.allPendingJobs.iterator()
                while (it.hasNext()) {
                    Log.e("PostCall", "job = " + it.next())
                }
                jobScheduler.cancelAll()
            }
            if ((if (Build.VERSION.SDK_INT >= 24) jobScheduler.getPendingJob(666) else null) != null) {
                jobScheduler.cancel(666)
            }
            jobScheduler.schedule(minimumLatency.build())
            Log.e("PostCall", "Scheduled job successfully!")
        }
    }

    fun checkCallPhonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, "android.permission.READ_PHONE_STATE") == 0
    }

    companion object {
        @JvmField
        val INSTANCE: PostCall = PostCall()
    }
}