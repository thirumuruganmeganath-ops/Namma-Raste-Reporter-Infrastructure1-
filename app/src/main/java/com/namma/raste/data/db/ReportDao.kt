package com.namma.raste.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.namma.raste.data.model.Report

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): LiveData<List<Report>>

    @Query("SELECT * FROM reports WHERE ticketId = :ticketId LIMIT 1")
    suspend fun getReportByTicketId(ticketId: String): Report?

    @Query("SELECT COUNT(*) FROM reports")
    suspend fun getReportCount(): Int
    @Query("UPDATE reports SET status = 'RESOLVED' WHERE ticketId = :ticketId")
    suspend fun markAsResolved(ticketId: String)
}
