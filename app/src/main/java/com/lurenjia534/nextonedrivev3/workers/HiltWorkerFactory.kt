package com.lurenjia534.nextonedrivev3.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.lurenjia534.nextonedrivev2.workers.TokenRefreshWorker
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiltWorkerFactory @Inject constructor(
    private val tokenManager: TokenManager
) : WorkerFactory() {
    
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when(workerClassName) {
            TokenRefreshWorker::class.java.name ->
                TokenRefreshWorker(appContext, workerParameters, tokenManager)
            else -> null
        }
    }
} 