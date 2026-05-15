package com.namma.raste.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.namma.raste.data.model.Report

@Database(entities = [Report::class], version = 4, exportSchema = false)
abstract class NammaRasteDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile private var INSTANCE: NammaRasteDatabase? = null

        fun getDatabase(context: Context): NammaRasteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    NammaRasteDatabase::class.java,
                    "namma_raste_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}