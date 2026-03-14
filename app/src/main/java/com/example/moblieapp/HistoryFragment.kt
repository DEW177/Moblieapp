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
import java.util.Calendar // 🔥 เพิ่ม Calendar เข้ามาเพื่อดึงเดือนปัจจุบัน

class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var spinnerMonth: Spinner
    private lateinit var spinnerWalletFilter: Spinner
    private lateinit var edtSearch: EditText
    private lateinit var layoutEmptyState: LinearLayout

    private var allTransactions = listOf<Transaction>()
    private var allWallets = listOf<Wallet>()
    private var filterWalletList = mutableListOf<Wallet?>()
    private var currentSearchQuery = ""

    private val months = arrayOf(
        "ดูทั้งหมด", "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
        "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        spinnerMonth = view.findViewById(R.id.spinnerMonth)
        spinnerWalletFilter = view.findViewById(R.id.spinnerWalletFilter)
        edtSearch = view.findViewById(R.id.edtSearch)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TransactionAdapter()
        recyclerView.adapter = adapter

        val spinnerMonthAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, months)
        spinnerMonth.adapter = spinnerMonthAdapter

        // 🔥 เซ็ตค่าเริ่มต้นของ Dropdown ให้เป็น "เดือนปัจจุบัน"
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        spinnerMonth.setSelection(currentMonth)

        spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterData()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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

            val realType = if (transaction.type == 4 || transaction.type == 5) 3 else transaction.type
            bundle.putInt("type", realType)

            bundle.putDouble("amount", transaction.amount)
            bundle.putString("category", if (realType == 3) "โอนเงิน" else transaction.category)
            bundle.putString("note", transaction.note)
            bundle.putString("date", transaction.date)

            if (transaction.type == 5) {
                bundle.putInt("walletId", transaction.toWalletId ?: 1)
                bundle.putInt("toWalletId", transaction.walletId)
            } else {
                bundle.putInt("walletId", transaction.walletId)
                transaction.toWalletId?.let { bundle.putInt("toWalletId", it) }
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

            val walletNames = mutableListOf("ดูทุกกระเป๋า")
            filterWalletList.clear()
            filterWalletList.add(null)

            for (w in allWallets) {
                val prefix = if (w.type == 0) "💰" else "💳"
                walletNames.add("$prefix ${w.name}")
                filterWalletList.add(w)
            }

            withContext(Dispatchers.Main) {
                val walletSpinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
                spinnerWalletFilter.adapter = walletSpinnerAdapter

                // ให้ระบบจำตำแหน่งเดิมของกระเป๋าถ้ามีการเลือกค้างไว้
                val currentWalletSelection = spinnerWalletFilter.selectedItemPosition
                if (currentWalletSelection >= 0 && currentWalletSelection < walletNames.size) {
                    spinnerWalletFilter.setSelection(currentWalletSelection)
                }

                filterData()
            }
        }
    }

    private fun filterData() {
        val monthIndex = spinnerMonth.selectedItemPosition
        val walletIndex = spinnerWalletFilter.selectedItemPosition

        // 🔥 กรองข้อมูลตามเดือนที่เลือก (ใช้ trim() ดักจับกรณีมีช่องว่างติดมาด้วย)
        val filteredByMonth = if (monthIndex <= 0) {
            allTransactions
        } else {
            allTransactions.filter { transaction ->
                val parts = transaction.date.split("/")
                parts.size == 3 && parts[1].trim() == monthIndex.toString()
            }
        }

        val filteredBySearch = if (currentSearchQuery.isEmpty()) {
            filteredByMonth
        } else {
            filteredByMonth.filter { transaction ->
                transaction.note.contains(currentSearchQuery, ignoreCase = true) ||
                        transaction.category.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        val selectedWallet = if (walletIndex > 0 && walletIndex < filterWalletList.size) filterWalletList[walletIndex] else null

        val filteredForWallet = if (selectedWallet == null) {
            filteredBySearch
        } else {
            filteredBySearch.filter { transaction ->
                transaction.walletId == selectedWallet.id ||
                        (transaction.type == 3 && transaction.toWalletId == selectedWallet.id)
            }
        }

        val displayList = mutableListOf<Transaction>()
        for (t in filteredForWallet) {
            if (t.type == 3 && t.toWalletId != null) {
                val srcWallet = allWallets.find { it.id == t.walletId }
                val destWallet = allWallets.find { it.id == t.toWalletId }

                if (selectedWallet == null || selectedWallet.id == t.walletId) {
                    displayList.add(t.copy(
                        type = 4,
                        category = "โอนไป ${destWallet?.name ?: "?"}"
                    ))
                }

                if (selectedWallet == null || selectedWallet.id == t.toWalletId) {
                    displayList.add(t.copy(
                        type = 5,
                        walletId = t.toWalletId,
                        toWalletId = t.walletId,
                        category = "รับโอนจาก ${srcWallet?.name ?: "?"}"
                    ))
                }
            } else {
                displayList.add(t)
            }
        }

        adapter.setData(displayList, allWallets)

        if (displayList.isEmpty()) {
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
        val realTransaction = allTransactions.find { it.id == transaction.id }
        if (realTransaction != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                db.transactionDao().deleteTransaction(realTransaction)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "ลบแล้ว", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
        }
    }
}