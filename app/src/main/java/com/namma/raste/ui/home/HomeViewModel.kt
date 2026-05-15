package com.namma.raste.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.namma.raste.data.db.NammaRasteDatabase
import com.namma.raste.data.model.Report
import com.namma.raste.data.repository.ReportRepository
import com.namma.raste.utils.FirestoreHelper
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReportRepository(
        NammaRasteDatabase.getDatabase(application).reportDao()
    )

    // Raw list from Room DB — always live
    val allReports = repository.allReports

    // Current active filter
    private val _activeFilter = MutableLiveData<String>("ALL")
    val activeFilter: LiveData<String> = _activeFilter

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun getFilteredReports(reports: List<Report>, filter: String): List<Report> {
        return when (filter) {
            "POTHOLE"     -> reports.filter { it.issueType == "POTHOLE" }
            "STREETLIGHT" -> reports.filter { it.issueType == "STREETLIGHT" }
            "OTHER"       -> reports.filter { it.issueType.startsWith("OTHER") }
            "HIGH"        -> reports.filter { it.severity == "HIGH" }
            "MEDIUM"      -> reports.filter { it.severity == "MEDIUM" }
            "LOW"         -> reports.filter { it.severity == "LOW" }
            "RESOLVED"    -> reports.filter { it.status == "RESOLVED" }
            else          -> reports
        }
    }

    fun markAsResolved(ticketId: String) {
        viewModelScope.launch {
            repository.markAsResolved(ticketId)
            FirestoreHelper.updateStatus(ticketId, "RESOLVED")
            android.util.Log.d("STATUS", "Marked as resolved: $ticketId")
        }
    }
}