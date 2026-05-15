package com.namma.raste.data.repository

import androidx.lifecycle.LiveData
import com.namma.raste.data.db.ReportDao
import com.namma.raste.data.model.Report

class ReportRepository(private val dao: ReportDao) {
    val allReports: LiveData<List<Report>> = dao.getAllReports()
    suspend fun insertReport(report: Report) = dao.insertReport(report)
    suspend fun getReportByTicketId(ticketId: String): Report? = dao.getReportByTicketId(ticketId)
    suspend fun getReportCount(): Int = dao.getReportCount()

    suspend fun markAsResolved(ticketId: String) = dao.markAsResolved(ticketId)
}
