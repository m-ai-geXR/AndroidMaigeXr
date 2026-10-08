package com.xraiassistant.data.background

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Hands replies to WorkManager and reads back what it finished. */
@Singleton
class BackgroundReplies @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val store: BackgroundReplyStore by lazy { BackgroundReplyStore.from(context) }

    fun start(job: BackgroundReplyStore.Job) {
        store.saveJob(job)
        BackgroundReplyWorker.enqueue(context, job.id)
    }

    fun cancel(jobId: String) {
        BackgroundReplyWorker.cancel(context, jobId)
        store.remove(jobId)
    }
}
