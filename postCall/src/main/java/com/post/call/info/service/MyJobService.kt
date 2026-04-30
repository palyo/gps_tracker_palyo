package com.post.call.info.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.post.call.info.service.PostCall

class MyJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        try {
            PostCall.INSTANCE.startPostCall(applicationContext)
        } catch (ignored: Exception) {}
        return false
    }
}
