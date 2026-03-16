package com.example.moblieapp

// ไม่ต้องมี @Entity หรือ @PrimaryKey แล้วครับ เพราะเราใช้ Firebase 100%
data class Transaction(
    var id: String = "",
    var type: Int = 1,
    var amount: Double = 0.0,
    var category: String = "",
    var note: String = "",
    var date: String = "",
    var walletId: String = "",
    var toWalletId: String? = null
)