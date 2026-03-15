package com.rahees.quickscan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val displayValue: String,
    val format: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
