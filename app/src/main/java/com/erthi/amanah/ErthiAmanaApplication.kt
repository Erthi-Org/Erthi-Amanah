package com.erthi.amanah

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.erthi.amanah.data.worker.RestoreRecoveryWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ErthiAmanaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Resume any restore interrupted after DB commit (journal in Room → worker completes moves)
        WorkManager.getInstance(this).enqueueUniqueWork(
            "restore_recovery",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RestoreRecoveryWorker>().build(),
        )
    }
}
