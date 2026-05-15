package com.namma.raste.ui.preview

import android.app.Application
import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import com.namma.raste.data.db.NammaRasteDatabase
import com.namma.raste.data.model.Report
import com.namma.raste.data.repository.ReportRepository
import com.namma.raste.utils.FirestoreHelper
import com.namma.raste.utils.TicketIdGenerator
import kotlinx.coroutines.launch

class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReportRepository(
        NammaRasteDatabase.getDatabase(application).reportDao()
    )

    private val _submitResult = MutableLiveData<Result<String>>()
    val submitResult: LiveData<Result<String>> = _submitResult

    fun submitReport(
        photoPath: String,
        issueType: String,
        severity: String,
        latitude: Double,
        longitude: Double,
        description: String = ""
    ) {
        viewModelScope.launch {
            try {
                val count = repository.getReportCount()
                val ticketId = TicketIdGenerator.generate(count)
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

                val report = Report(
                    ticketId = ticketId,
                    userId = userId,
                    photoPath = photoPath,
                    issueType = issueType,
                    severity = severity,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = System.currentTimeMillis(),
                    status = "SUBMITTED",
                    description = description
                )

                // Step 1 — Save to Room DB (local)
                repository.insertReport(report)
                android.util.Log.d("SUBMIT", "Saved to Room DB: $ticketId")

                // Step 2 — Save to Firestore (cloud)
                FirestoreHelper.saveReport(
                    report = report,
                    onSuccess = {
                        android.util.Log.d("SUBMIT", "Saved to Firestore: $ticketId")
                    },
                    onFailure = { e ->
                        android.util.Log.e("SUBMIT", "Firestore failed (Room OK): ${e.message}")
                    }
                )

                // Always post success — Room is primary storage
                _submitResult.postValue(Result.success(ticketId))

            } catch (e: Exception) {
                android.util.Log.e("SUBMIT", "Submit failed: ${e.message}")
                _submitResult.postValue(Result.failure(e))
            }
        }
    }
}