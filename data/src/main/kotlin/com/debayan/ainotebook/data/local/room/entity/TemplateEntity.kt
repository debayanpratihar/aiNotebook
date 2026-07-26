package com.debayan.ainotebook.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val templateId: String,
    val name: String,
    val category: String,
    val backgroundType: String,
    val isDarkVariant: Boolean = false,
)
