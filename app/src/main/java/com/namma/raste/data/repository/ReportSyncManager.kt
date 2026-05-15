package com.namma.raste.data.sync

import com.namma.raste.data.local.ReportDao
import com.namma.raste.data.model.Report
import com.namma.raste.data.repository.FirestoreReportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ReportSyncManager
 *
 * Offline-first strategy:
 *  1. Room is the source of truth for the local UI.
 *  2. Every write goes to Room first, then Firestore.
 *  3. On app launch (or explicit refresh), remote reports are pulled
 *     and merged into Room so the device stays up to date.
 */
@Singleton
class ReportSyncManager @Inject constructor(
    private val reportDao: ReportDao,
    private val firestoreRepo: FirestoreReportRepository
) {

    /**
     * Save a new report locally AND push it to Firestore.
     * Returns true if both operations succeeded.
     */
    suspend fun submitReport(report: Report): Boolean = withContext(Dispatchers.IO) {
        // 1. Write to local Room DB first (always succeeds offline)
        reportDao.insertReport(report)

        // 2. Push to Firestore (may fail if offline — that's OK)
        firestoreRepo.uploadReport(report).isSuccess
    }

    /**
     * Pull all remote reports for this user and upsert them into Room.
     * Call this on app start or when the user pulls-to-refresh.
     */
    suspend fun syncFromFirestore(userId: String): SyncResult = withContext(Dispatchers.IO) {
        val result = firestoreRepo.fetchReportsForUser(userId)

        result.fold(
            onSuccess = { remoteReports ->
                // Upsert: inserts new, replaces existing by ticketId primary key
                reportDao.insertReports(remoteReports)
                SyncResult.Success(synced = remoteReports.size)
            },
            onFailure = { error ->
                SyncResult.Failure(error.message ?: "Unknown sync error")
            }
        )
    }

    /**
     * Push all local reports that haven't been synced yet.
     * Useful after the device comes back online.
     */
    suspend fun pushPendingReports(userId: String): SyncResult = withContext(Dispatchers.IO) {
        val localReports = reportDao.getReportsByUser(userId)

        if (localReports.isEmpty()) return@withContext SyncResult.Success(synced = 0)

        // Batch in chunks of 500 (Firestore limit)
        val chunks = localReports.chunked(500)
        var totalSynced = 0
        var lastError: String? = null

        chunks.forEach { chunk ->
            firestoreRepo.uploadReports(chunk).fold(
                onSuccess = { totalSynced += chunk.size },
                onFailure = { lastError = it.message }
            )
        }

        if (lastError != null && totalSynced == 0) {
            SyncResult.Failure(lastError!!)
        } else {
            SyncResult.Success(synced = totalSynced)
        }
    }

    /**
     * Update a report's status both locally and remotely.
     */
    suspend fun updateReportStatus(ticketId: String, newStatus: String) = withContext(Dispatchers.IO) {
        reportDao.updateStatus(ticketId, newStatus)
        firestoreRepo.updateStatus(ticketId, newStatus)
    }
}

sealed class SyncResult {
    data class Success(val synced: Int) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}
