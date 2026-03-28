package aanibrothers.tracker.io.afterCall.service

import android.app.job.JobParameters
import android.app.job.JobService
import aanibrothers.tracker.io.afterCall.PostCall

class MyJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        jobFinished(params, false)
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        try {
            PostCall.INSTANCE.startPostCall(applicationContext)
        } catch (_: Exception) {
        }
        return false
    }
}
