package com.example.moblieapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: Int,          // 1 = รายรับ, 2 = รายจ่าย (หรือใช้ boolean ก็ได้)
    val amount: Double,
    val category: String,
    val note: String
)