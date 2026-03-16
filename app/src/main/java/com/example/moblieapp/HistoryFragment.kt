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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.VISIBLE
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

        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
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
            bundle.putString("id", transaction.id)

            val realType = if (transaction.type == 4 || transaction.type == 5) 3 else transaction.type
            bundle.putInt("type", realType)

            bundle.putDouble("amount", transaction.amount)
            bundle.putString("category", if (realType == 3) "โอนเงิน" else transaction.category)
            bundle.putString("note", transaction.note)
            bundle.putString("date", transaction.date)

            // 🔥 แก้ไขส่วนนี้ให้ส่ง ID แบบ String
            if (transaction.type == 5) {
                bundle.putString("walletId", transaction.toWalletId ?: "")
                bundle.putString("toWalletId", transaction.walletId)
            } else {
                bundle.putString("walletId", transaction.walletId)
                transaction.toWalletId?.let { bundle.putString("toWalletId", it) }
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

    private fun loadData() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val dbFire = FirebaseFirestore.getInstance()

        // โหลดกระเป๋าเงินก่อน
        dbFire.collection("users").document(userId).collection("wallets")
            .addSnapshotListener { walletSnapshots, wError ->
                if (wError != null || walletSnapshots == null || !isAdded) return@addSnapshotListener

                val wList = mutableListOf<Wallet>()
                for (doc in walletSnapshots) {
                    wList.add(Wallet(doc.id, doc.getString("name") ?: "", doc.getLong("type")?.toInt() ?: 0, doc.getDouble("balance") ?: 0.0, doc.getDouble("creditLimit") ?: 0.0))
                }
                allWallets = wList

                val walletNames = mutableListOf("ดูทุกกระเป๋า")
                filterWalletList.clear()
                filterWalletList.add(null)

                for (w in allWallets) {
                    val prefix = if (w.type == 0) "💰" else "💳"
                    walletNames.add("$prefix ${w.name}")
                    filterWalletList.add(w)
                }

                if (isAdded) {
                    val currentWalletSelection = spinnerWalletFilter.selectedItemPosition
                    val walletSpinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
                    spinnerWalletFilter.adapter = walletSpinnerAdapter

                    if (currentWalletSelection in walletNames.indices) {
                        spinnerWalletFilter.setSelection(currentWalletSelection)
                    }
                }

                // โหลดรายการ
                dbFire.collection("users").document(userId).collection("transactions")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { transSnapshots, tError ->
                        if (tError != null || transSnapshots == null || !isAdded) return@addSnapshotListener

                        val tList = mutableListOf<Transaction>()
                        for (doc in transSnapshots) {
                            val id = doc.id
                            val type = doc.getLong("type")?.toInt() ?: 1
                            val amount = doc.getDouble("amount") ?: 0.0
                            val category = doc.getString("category") ?: ""
                            val note = doc.getString("note") ?: ""
                            val date = doc.getString("date") ?: ""
                            val walletId = doc.get("walletId")?.toString() ?: ""
                            val toWalletId = doc.get("toWalletId")?.toString()

                            tList.add(Transaction(id, type, amount, category, note, date, walletId, toWalletId))
                        }
                        allTransactions = tList
                        filterData()
                    }
            }
    }

    private fun filterData() {
        val monthIndex = spinnerMonth.selectedItemPosition
        val walletIndex = spinnerWalletFilter.selectedItemPosition

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
            val safeToWalletId = t.toWalletId // 🔥 ดึงมาเก็บใน val ก่อน เพื่อให้ Kotlin สบายใจ

            if (t.type == 3 && safeToWalletId != null) {
                val srcWallet = allWallets.find { it.id == t.walletId }
                val destWallet = allWallets.find { it.id == safeToWalletId }

                if (selectedWallet == null || selectedWallet.id == t.walletId) {
                    displayList.add(t.copy(
                        type = 4,
                        category = "โอนไป ${destWallet?.name ?: "?"}"
                    ))
                }

                if (selectedWallet == null || selectedWallet.id == safeToWalletId) {
                    displayList.add(t.copy(
                        type = 5,
                        walletId = safeToWalletId,
                        toWalletId = t.walletId,
                        category = "รับโอนจาก ${srcWallet?.name ?: "?"}"
                    ))
                }
            } else {
                displayList.add(t)
            }
        }

        val groupedByDate = displayList.groupBy { it.date }

        val sortedDates = groupedByDate.keys.sortedByDescending { dateStr ->
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val d = parts[0].trim().padStart(2, '0')
                val m = parts[1].trim().padStart(2, '0')
                val y = parts[2].trim()
                "$y$m$d"
            } else dateStr
        }

        val finalHistoryItems = mutableListOf<HistoryItem>()
        for (date in sortedDates) {
            val dailyTransactions = groupedByDate[date]!!
            var dailyTotal = 0.0

            for (t in dailyTransactions) {
                if (t.type == 1 || t.type == 5) dailyTotal += t.amount
                else if (t.type == 2 || t.type == 4) dailyTotal -= t.amount
            }

            finalHistoryItems.add(HistoryItem.DateHeader(date, dailyTotal))

            for (t in dailyTransactions) {
                finalHistoryItems.add(HistoryItem.TransactionItem(t))
            }
        }

        adapter.setData(finalHistoryItems, allWallets)

        if (finalHistoryItems.isEmpty()) {
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbFire = FirebaseFirestore.getInstance()

        dbFire.collection("users").document(userId)
            .collection("transactions").document(transaction.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "ลบข้อมูลสำเร็จ", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "ลบข้อมูลล้มเหลว", Toast.LENGTH_SHORT).show()
            }
    }
}