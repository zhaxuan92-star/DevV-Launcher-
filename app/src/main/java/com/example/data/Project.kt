package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val pythonCode: String,
    val visualDesignJson: String = "[]",
    val lastModified: Long = System.currentTimeMillis()
) : Serializable
