package com.momostack.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object EnrichmentScheduler {

    fun scheduleEnrichment(context: Context, itemId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<EnrichmentWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(EnrichmentWorker.KEY_ITEM_ID to itemId))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
