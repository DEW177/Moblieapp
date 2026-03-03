package com.example.moblieapp

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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
    private lateinit var spinnerMonth: Spinner
    private lateinit var edtSearch: EditText // 🔥 ตัวแปรช่องค้นหา
    private lateinit var layoutEmptyState: LinearLayout
    private var allTransactions = listOf<Transaction>()
    private var currentSearchQuery = "" // 🔥 เก็บคำค้นหาปัจจุบัน

    private val months = arrayOf(
        "ดูทั้งหมด", "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        edtSearch = view.findViewById(R.id.edtSearch) // 🔥 เชื่อมตัวแปร
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter()
        recyclerView.adapter = adapter

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spinnerMonth.adapter = spinnerAdapter

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterData(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 🔥 ดักจับการพิมพ์ในช่องค้นหา (พิมพ์ปุ๊บ กรองปั๊บ)
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                filterData(spinnerMonth.selectedItemPosition)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        adapter.onDeleteClick = { transaction ->
            showDeleteDialog(transaction)
        }

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
            allTransactions = db.transactionDao().getAllTransactions()
            withContext(Dispatchers.Main) {
                filterData(spinnerMonth.selectedItemPosition)
            }
        }
    }

    // 🔥 ปรับการกรองให้รองรับทั้งเดือนและการค้นหา
    private fun filterData(monthIndex: Int) {
        // 1. กรองตามเดือนก่อน
        val filteredByMonth = if (monthIndex == 0) {
            allTransactions
        } else {
            allTransactions.filter { transaction ->
                val parts = transaction.date.split("/")
                parts.size == 3 && parts[1] == monthIndex.toString()
            }
        }

        // 2. กรองตามคำค้นหาต่อ
        val finalFilteredList = if (currentSearchQuery.isEmpty()) {
            filteredByMonth
        } else {
            filteredByMonth.filter { transaction ->
                transaction.note.contains(currentSearchQuery, ignoreCase = true) ||
                        transaction.category.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        adapter.setData(finalFilteredList)

        // 3. จัดการ Empty State
        if (finalFilteredList.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

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