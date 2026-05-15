package com.namma.raste.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.namma.raste.data.model.Report
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val COLLECTION_REPORTS = "reports"
    }

    /**
     * Upload a single report to Firestore.
     * Uses ticketId as the document ID so re-uploads are idempotent.
     */
    suspend fun uploadReport(report: Report): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_REPORTS)
            .document(report.ticketId)
            .set(report.toMap(), SetOptions.merge())
            .await()
    }

    /**
     * Upload multiple reports in a single batch (max 500 per Firestore limit).
     */
    suspend fun uploadReports(reports: List<Report>): Result<Unit> = runCatching {
        require(reports.size <= 500) { "Batch size cannot exceed 500 documents" }
        val batch = firestore.batch()
        reports.forEach { report ->
            val ref = firestore.collection(COLLECTION_REPORTS).document(report.ticketId)
            batch.set(ref, report.toMap(), SetOptions.merge())
        }
        batch.commit().await()
    }

    /**
     * Fetch all reports belonging to a specific user.
     */
    suspend fun fetchReportsForUser(userId: String): Result<List<Report>> = runCatching {
        firestore.collection(COLLECTION_REPORTS)
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toReport() }
    }

    /**
     * Fetch a single report by ticketId.
     */
    suspend fun fetchReport(ticketId: String): Result<Report?> = runCatching {
        firestore.collection(COLLECTION_REPORTS)
            .document(ticketId)
            .get()
            .await()
            .toReport()
    }

    /**
     * Update only the status field of an existing report.
     */
    suspend fun updateStatus(ticketId: String, newStatus: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_REPORTS)
            .document(ticketId)
            .update("status", newStatus)
            .await()
    }

    /**
     * Delete a report from Firestore.
     */
    suspend fun deleteReport(ticketId: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_REPORTS)
            .document(ticketId)
            .delete()
            .await()
    }

    // ── Mapping helpers ────────────────────────────────────────────────────

    private fun Report.toMap(): Map<String, Any> = mapOf(
        "ticketId"     to ticketId,
        "userId"       to userId,
        "photoPath"    to photoPath,
        "issueType"    to issueType,
        "severity"     to severity,
        "latitude"     to latitude,
        "longitude"    to longitude,
        "timestamp"    to timestamp,
        "status"       to status,
        "aiConfidence" to aiConfidence,
        "description"  to description
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toReport(): Report? {
        return try {
            Report(
                ticketId     = getString("ticketId")     ?: return null,
                userId       = getString("userId")       ?: return null,
                photoPath    = getString("photoPath")    ?: "",
                issueType    = getString("issueType")    ?: "",
                severity     = getString("severity")     ?: "",
                latitude     = getDouble("latitude")     ?: 0.0,
                longitude    = getDouble("longitude")    ?: 0.0,
                timestamp    = getLong("timestamp")      ?: 0L,
                status       = getString("status")       ?: "SUBMITTED",
                aiConfidence = getLong("aiConfidence")?.toInt() ?: 0,
                description  = getString("description")  ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}
