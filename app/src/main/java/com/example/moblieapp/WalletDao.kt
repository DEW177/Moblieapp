package com.example.moblieapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface WalletDao {
    @Insert
    fun insertWallet(wallet: Wallet)

    @Update
    fun updateWallet(wallet: Wallet)

    @Delete
    fun deleteWallet(wallet: Wallet)

    @Query("SELECT * FROM wallet_table ORDER BY id ASC")
    fun getAllWallets(): List<Wallet>

    @Query("SELECT * FROM wallet_table WHERE id = :id LIMIT 1")
    fun getWalletById(id: Int): Wallet?
}