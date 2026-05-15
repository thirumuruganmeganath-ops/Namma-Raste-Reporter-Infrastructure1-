package com.namma.raste.utils

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.namma.raste.data.model.Report

object FirestoreHelper {

    private val db = Firebase.firestore
    private const val COLLECTION = "reports"

    // Save report to Firestore
    fun saveReport(report: Report, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val data = hashMapOf(
            "ticketId"    to report.ticketId,
            "userId"      to report.userId,
            "photoPath"   to report.photoPath,
            "issueType"   to report.issueType,
            "severity"    to report.severity,
            "latitude"    to report.latitude,
            "longitude"   to report.longitude,
            "timestamp"   to report.timestamp,
            "status"      to report.status,
            "description" to report.description,
            "aiConfidence" to report.aiConfidence
        )

        db.collection(COLLECTION)
            .document(report.ticketId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Delete report from Firestore
    fun deleteReport(ticketId: String) {
        db.collection(COLLECTION)
            .document(ticketId)
            .delete()
            .addOnFailureListener { e ->
                android.util.Log.e("FIRESTORE", "Delete failed: ${e.message}")
            }
    }

    // Update report status in Firestore
    fun updateStatus(ticketId: String, status: String) {
        db.collection(COLLECTION)
            .document(ticketId)
            .update("status", status)
            .addOnFailureListener { e ->
                android.util.Log.e("FIRESTORE", "Update failed: ${e.message}")
            }
    }
}