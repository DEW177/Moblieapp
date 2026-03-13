package com.example.moblieapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🔥 1. เพิ่ม Wallet::class เข้าไป และปรับ version เป็น 3
@Database(entities = [Transaction::class, Wallet::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    // 🔥 2. เพิ่ม WalletDao เข้ามาเพื่อให้เรียกใช้งานกระเป๋าเงินได้
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"
                )
                    .fallbackToDestructiveMigration() // ถ้าโครงสร้างเปลี่ยน จะล้างข้อมูลเก่าทิ้งอัตโนมัติ
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}