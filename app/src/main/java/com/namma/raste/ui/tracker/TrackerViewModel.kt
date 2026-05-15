package com.namma.raste.ui.tracker

import android.app.Application
import androidx.lifecycle.*
import com.namma.raste.data.db.NammaRasteDatabase
import com.namma.raste.data.model.Report
import com.namma.raste.data.repository.ReportRepository
import kotlinx.coroutines.launch

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportRepository(NammaRasteDatabase.getDatabase(application).reportDao())
    private val _report = MutableLiveData<Report?>()
    val report: LiveData<Report?> = _report

    fun searchTicket(ticketId: String) {
        viewModelScope.launch {
            _report.postValue(repository.getReportByTicketId(ticketId.trim()))
        }
    }
}
