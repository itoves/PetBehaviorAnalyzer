package com.petanalyzer.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "behavior_logs")
data class BehaviorLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "date_key")
    val dateKey: String,

    @ColumnInfo(name = "pet_type")
    val petType: String,

    @ColumnInfo(name = "behavior")
    val behavior: String,

    @ColumnInfo(name = "emotion")
    val emotion: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "snapshot_path")
    val snapshotPath: String? = null,
)
