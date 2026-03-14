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
    private lateinit var spinnerWalletFilter: Spinner // 🔥 เพิ่ม Spinner สำหรับกรองกระเป๋า
    private lateinit var edtSearch: EditText
    private lateinit var layoutEmptyState: LinearLayout

    private var allTransactions = listOf<Transaction>()
    private var allWallets = listOf<Wallet>()
    private var filterWalletList = mutableListOf<Wallet?>() // เก็บรายชื่อกระเป๋าเพื่อใช้อ้างอิงตอนกรอง
    private var currentSearchQuery = ""

    private val months = arrayOf(
        "ดูทั้งหมด", "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        spinnerWalletFilter = view.findViewById(R.id.spinnerWalletFilter) // 🔥 ผูกตัวแปร
        edtSearch = view.findViewById(R.id.edtSearch)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter()
        recyclerView.adapter = adapter

        val spinnerMonthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spinnerMonth.adapter = spinnerMonthAdapter

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterData() // 🔥 แก้ให้เรียกฟังก์ชันแบบไม่ต้องส่ง parameter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 🔥 ดักจับ Event ตอนเปลี่ยนกระเป๋าเงินเพื่อกรองข้อมูล
        spinnerWalletFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                filterData()
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
            bundle.putInt("walletId", transaction.walletId)

            transaction.toWalletId?.let {
                bundle.putInt("toWalletId", it)
            }

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
            allWallets = db.walletDao().getAllWallets()

            // 🔥 เตรียมข้อมูลสำหรับ Dropdown กรองกระเป๋าเงิน
            val walletNames = mutableListOf("ดูทุกกระเป๋า")
            filterWalletList.clear()
            filterWalletList.add(null) // Index 0 คือดูทั้งหมด (ไม่ระบุ)

            for (w in allWallets) {
                val prefix = if (w.type == 0) "💰" else "💳"
                walletNames.add("$prefix ${w.name}")
                filterWalletList.add(w)
            }

            withContext(Dispatchers.Main) {
                val walletSpinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
                spinnerWalletFilter.adapter = walletSpinnerAdapter

                filterData()
            }
        }
    }

    private fun filterData() {
        // เช็คตำแหน่งล่าสุดของ Dropdown ทั้ง 2 อัน
        val monthIndex = spinnerMonth.selectedItemPosition
        val walletIndex = spinnerWalletFilter.selectedItemPosition

        // 1. กรองเดือน
        val filteredByMonth = if (monthIndex <= 0) {
            allTransactions
        } else {
            allTransactions.filter { transaction ->
                val parts = transaction.date.split("/")
                parts.size == 3 && parts[1] == monthIndex.toString()
            }
        }

        // 2. กรองคำค้นหา
        val filteredBySearch = if (currentSearchQuery.isEmpty()) {
            filteredByMonth
        } else {
            filteredByMonth.filter { transaction ->
                transaction.note.contains(currentSearchQuery, ignoreCase = true) ||
                        transaction.category.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // 3. 🔥 กรองกระเป๋าเงิน
        val selectedWallet = if (walletIndex > 0 && walletIndex < filterWalletList.size) filterWalletList[walletIndex] else null

        val finalFilteredList = if (selectedWallet == null) {
            filteredBySearch
        } else {
            filteredBySearch.filter { transaction ->
                // รายการจะแสดงก็ต่อเมื่อเกี่ยวข้องกับกระเป๋าที่เลือก (เป็นต้นทาง หรือ ปลายทางตอนโอนเงิน)
                transaction.walletId == selectedWallet.id ||
                        (transaction.type == 3 && transaction.toWalletId == selectedWallet.id)
            }
        }

        adapter.setData(finalFilteredList, allWallets)

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
                loadData() // โหลดใหม่และกรองอัตโนมัติ
            }
        }
    }
}