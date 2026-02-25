package com.example.moblieapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ตั้งค่า RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter()
        recyclerView.adapter = adapter

        // เมื่อมีการกดที่รายการใน Adapter
        adapter.onDeleteClick = { transaction ->
            showDeleteDialog(transaction)
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val list = db.transactionDao().getAllTransactions()
            withContext(Dispatchers.Main) {
                adapter.setData(list)
            }
        }
    }

    private fun showDeleteDialog(transaction: Transaction) {
        // Inflate หน้าตา Dialog ที่คุณสร้างไว้
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        val btnNo = dialogView.findViewById<Button>(R.id.btnNo)
        val btnYes = dialogView.findViewById<Button>(R.id.btnYes)

        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        btnYes.setOnClickListener {
            deleteTransaction(transaction)
            dialog.dismiss()
        }
    }

    private fun deleteTransaction(transaction: Transaction) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            // คำสั่งลบใน Room Database
            db.transactionDao().deleteTransaction(transaction)

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "ลบรายการเรียบร้อย", Toast.LENGTH_SHORT).show()
                loadData() // โหลดข้อมูลใหม่ทันทีหลังลบเสร็จ
            }
        }
    }
}