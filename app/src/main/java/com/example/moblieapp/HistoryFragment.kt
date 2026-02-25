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

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter()
        recyclerView.adapter = adapter

        // 🔥 คำสั่งที่ 1: เมื่อกดรูปถังขยะ ให้โชว์หน้าต่างยืนยันการลบ
        adapter.onDeleteClick = { transaction ->
            showDeleteDialog(transaction)
        }

        // 🔥 คำสั่งที่ 2: เมื่อกดที่ตัวรายการ ให้ส่งข้อมูลไปหน้าแก้ไข
        adapter.onItemClick = { transaction ->
            val bundle = Bundle()
            bundle.putInt("id", transaction.id)
            bundle.putInt("type", transaction.type)
            bundle.putDouble("amount", transaction.amount)
            bundle.putString("category", transaction.category)
            bundle.putString("note", transaction.note)
            bundle.putString("date", transaction.date)

            val addFragment = AddTransactionFragment()
            addFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, addFragment)
                .addToBackStack(null)
                .commit()
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

    // ฟังก์ชันโชว์หน้าต่างลบ
    private fun showDeleteDialog(transaction: Transaction) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialog.show()

        dialogView.findViewById<Button>(R.id.btnNo).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<Button>(R.id.btnYes).setOnClickListener {
            deleteFromDb(transaction)
            dialog.dismiss()
        }
    }

    // ฟังก์ชันสั่งลบจาก Database
    private fun deleteFromDb(transaction: Transaction) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            db.transactionDao().deleteTransaction(transaction)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "ลบแล้ว", Toast.LENGTH_SHORT).show()
                loadData()
            }
        }
    }
}