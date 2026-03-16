package com.example.moblieapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_table")
data class Wallet(
    @PrimaryKey(autoGenerate = true)
    var id: String = "",
    val name: String,     // ชื่อกระเป๋า เช่น "เงินสด", "ธนาคาร KBank", "บัตรเครดิต"
    val type: Int,        // ประเภทกระเป๋า: 0 = ทรัพย์สิน (เงินสด/ธนาคาร), 1 = บัตรเครดิต (หนี้สิน)
    val balance: Double,   // ยอดเงินตั้งต้น (ถ้าเป็นบัตรเครดิตจะเป็น 0.0)
    val creditLimit: Double = 0.0
)