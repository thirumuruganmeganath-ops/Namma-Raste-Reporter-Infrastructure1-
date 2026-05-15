package com.namma.raste.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey val ticketId: String,
    val userId: String,
    val photoPath: String,
    val issueType: String,
    val severity: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val status: String = "SUBMITTED",
    val aiConfidence: Int = 0,
    val description: String = ""
)