package com.example.moblieapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.DatePickerDialog
import java.util.Calendar
import android.widget.TextView

class AddTransactionFragment : Fragment(R.layout.fragment_add_transaction) {

    private var currentTransactionId: Int = 0
    private lateinit var tvDate: TextView
    private var selectedDate: String = ""
    private lateinit var btnIncome: Button
    private lateinit var btnExpense: Button
    private lateinit var btnTransfer: Button
    private lateinit var edtAmount: EditText
    private lateinit var radioGroupIncome: RadioGroup
    private lateinit var radioGroupExpense: RadioGroup
    private lateinit var edtNote: EditText
    private lateinit var btnSave: Button
    private lateinit var spinnerWallet: Spinner
    private lateinit var spinnerToWallet: Spinner
    private lateinit var tvToWalletLabel: TextView
    private var walletList = listOf<Wallet>()
    private var selectedWalletIdFromEdit: Int = 0
    private var isExpense: Boolean = true
    private var isTransfer: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        setupListeners()
    }

    private fun init(view: View) {
        btnIncome = view.findViewById(R.id.btnIncome)
        btnExpense = view.findViewById(R.id.btnExpense)
        btnTransfer = view.findViewById(R.id.btnTransfer)
        edtAmount = view.findViewById(R.id.edtAmount)
        radioGroupIncome = view.findViewById(R.id.radioGroupIncome)
        radioGroupExpense = view.findViewById(R.id.radioGroupExpense)
        edtNote = view.findViewById(R.id.edtNote)
        btnSave = view.findViewById(R.id.btnSave)
        tvDate = view.findViewById(R.id.tvDate)
        spinnerWallet = view.findViewById(R.id.spinnerWallet)
        spinnerToWallet = view.findViewById(R.id.spinnerToWallet)
        tvToWalletLabel = view.findViewById(R.id.tvToWalletLabel)

        updateTypeSelection()

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        selectedDate = "$day/${month + 1}/$year"
        tvDate.text = "วันที่: $selectedDate"

        tvDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    tvDate.text = "วันที่: $selectedDate"
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        arguments?.let { bundle ->
            currentTransactionId = bundle.getInt("id", 0)

            if (currentTransactionId != 0) {
                isExpense = (bundle.getInt("type", 2) == 2)
                updateTypeSelection()

                val amountVal = bundle.getDouble("amount", 0.0)
                edtAmount.setText(if (amountVal % 1.0 == 0.0) amountVal.toInt().toString() else amountVal.toString())

                edtNote.setText(bundle.getString("note", ""))
                selectedWalletIdFromEdit = bundle.getInt("walletId", 0)

                val dateStr = bundle.getString("date", "")
                if (dateStr.isNotEmpty()) {
                    selectedDate = dateStr
                    tvDate.text = "วันที่: $selectedDate"
                }

                btnSave.text = "อัปเดตรายการ"

                val categoryStr = bundle.getString("category", "")
                val activeRadioGroup = if (isExpense) radioGroupExpense else radioGroupIncome

                for (i in 0 until activeRadioGroup.childCount) {
                    val rb = activeRadioGroup.getChildAt(i) as? RadioButton
                    if (rb?.text.toString() == categoryStr) {
                        rb?.isChecked = true
                        break
                    }
                }
            }
        }

        loadWallets()
    }

    private fun loadWallets() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            var wallets = db.walletDao().getAllWallets()

            if (wallets.isEmpty()) {
                db.walletDao().insertWallet(Wallet(name = "เงินสด", type = 0, balance = 0.0))
                db.walletDao().insertWallet(Wallet(name = "บัญชีธนาคาร", type = 0, balance = 0.0))
                db.walletDao().insertWallet(Wallet(name = "บัตรเครดิต", type = 1, balance = 0.0))
                wallets = db.walletDao().getAllWallets()
            }

            walletList = wallets

            withContext(Dispatchers.Main) {
                updateWalletSpinner()

                // spinnerToWallet ใช้ทุกกระเป๋า
                val allWalletNames = walletList.map {
                    if (it.type == 0) "💰 ${it.name}" else "💳 ${it.name}"
                }
                val adapterTo = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, allWalletNames)
                spinnerToWallet.adapter = adapterTo

                if (selectedWalletIdFromEdit != 0) {
                    val selectedIndex = walletList.indexOfFirst { it.id == selectedWalletIdFromEdit }
                    if (selectedIndex >= 0) spinnerWallet.setSelection(selectedIndex)
                }
            }
        }
    }

    private fun updateWalletSpinner() {
        if (walletList.isEmpty()) return

        val filteredWallets = if (isExpense || isTransfer) {
            walletList
        } else {
            walletList.filter { it.type == 0 } // รายรับ เลือกได้แค่กระเป๋าธรรมดา
        }

        val walletNames = filteredWallets.map {
            if (it.type == 0) "💰 ${it.name}" else "💳 ${it.name}"
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
        spinnerWallet.adapter = adapter
    }

    private fun setupListeners() {
        btnIncome.setOnClickListener {
            isExpense = false
            isTransfer = false
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnExpense.setOnClickListener {
            isExpense = true
            isTransfer = false
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnTransfer.setOnClickListener {
            isTransfer = true
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnSave.setOnClickListener { saveTransaction() }
    }

    private fun updateTypeSelection() {
        val activeColor = Color.parseColor("#78C2A4")
        val inactiveColor = Color.parseColor("#DDDDDD")

        btnIncome.setBackgroundColor(inactiveColor)
        btnIncome.setTextColor(Color.BLACK)
        btnExpense.setBackgroundColor(inactiveColor)
        btnExpense.setTextColor(Color.BLACK)
        btnTransfer.setBackgroundColor(inactiveColor)
        btnTransfer.setTextColor(Color.BLACK)

        radioGroupExpense.visibility = View.GONE
        radioGroupIncome.visibility = View.GONE
        spinnerToWallet.visibility = View.GONE
        tvToWalletLabel.visibility = View.GONE

        when {
            isTransfer -> {
                btnTransfer.setBackgroundColor(activeColor)
                btnTransfer.setTextColor(Color.WHITE)
                spinnerToWallet.visibility = View.VISIBLE
                tvToWalletLabel.visibility = View.VISIBLE
            }
            isExpense -> {
                btnExpense.setBackgroundColor(activeColor)
                btnExpense.setTextColor(Color.WHITE)
                radioGroupExpense.visibility = View.VISIBLE
            }
            else -> {
                btnIncome.setBackgroundColor(activeColor)
                btnIncome.setTextColor(Color.WHITE)
                radioGroupIncome.visibility = View.VISIBLE
            }
        }
    }

    private fun saveTransaction() {
        val amountText = edtAmount.text.toString()
        if (amountText.isEmpty()) {
            edtAmount.error = "กรุณากรอกจำนวนเงิน"
            return
        }
        val amount = amountText.toDouble()

        // 🔥 ใช้ filtered list ให้ตรงกับ spinner ที่แสดงอยู่
        val filteredWallets = if (isExpense || isTransfer) walletList else walletList.filter { it.type == 0 }
        val selectedWalletIndex = spinnerWallet.selectedItemPosition
        val targetWalletId = if (selectedWalletIndex >= 0 && filteredWallets.isNotEmpty())
            filteredWallets[selectedWalletIndex].id else 1

        if (isTransfer) {
            val toWalletIndex = spinnerToWallet.selectedItemPosition
            val toWalletId = if (toWalletIndex >= 0 && walletList.isNotEmpty())
                walletList[toWalletIndex].id else 1

            if (targetWalletId == toWalletId) {
                Toast.makeText(requireContext(), "กระเป๋าต้นทางและปลายทางต้องไม่เหมือนกัน", Toast.LENGTH_SHORT).show()
                return
            }

            val transaction = Transaction(
                id = currentTransactionId,
                type = 3,
                amount = amount,
                category = "โอนเงิน",
                note = edtNote.text.toString(),
                date = selectedDate,
                walletId = targetWalletId,
                toWalletId = toWalletId
            )

            val db = AppDatabase.getDatabase(requireContext())
            lifecycleScope.launch(Dispatchers.IO) {
                db.transactionDao().insertTransaction(transaction)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "โอนเงินเรียบร้อย", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            return
        }

        // Income / Expense
        val activeRadioGroup = if (isExpense) radioGroupExpense else radioGroupIncome
        val selectedId = activeRadioGroup.checkedRadioButtonId
        var category = "อื่นๆ"
        if (selectedId != -1) {
            val selectedRadioButton = view?.findViewById<RadioButton>(selectedId)
            category = selectedRadioButton?.text.toString() ?: "อื่นๆ"
        }

        val type = if (isExpense) 2 else 1
        val transaction = Transaction(
            id = currentTransactionId,
            type = type,
            amount = amount,
            category = category,
            note = edtNote.text.toString(),
            date = selectedDate,
            walletId = targetWalletId
        )

        val db = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch(Dispatchers.IO) {
            if (currentTransactionId == 0) {
                db.transactionDao().insertTransaction(transaction)
            } else {
                db.transactionDao().updateTransaction(transaction)
            }
            withContext(Dispatchers.Main) {
                val message = if (currentTransactionId == 0) "บันทึกข้อมูลเรียบร้อย" else "อัปเดตข้อมูลเรียบร้อย"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}