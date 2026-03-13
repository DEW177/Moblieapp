package com.example.moblieapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: Int,          // 1 = รายรับ, 2 = รายจ่าย, 3 = โอนระหว่างกระเป๋า
    val amount: Double,
    val category: String,
    val note: String,
    val date: String,
    val walletId: Int,
    val toWalletId: Int? = null
)