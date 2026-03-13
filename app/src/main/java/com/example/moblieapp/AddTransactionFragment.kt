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
    private lateinit var edtAmount: EditText

    private lateinit var radioGroupIncome: RadioGroup
    private lateinit var radioGroupExpense: RadioGroup

    private lateinit var edtNote: EditText
    private lateinit var btnSave: Button

    // 🔥 ตัวแปรสำหรับกระเป๋าเงิน
    private lateinit var spinnerWallet: Spinner
    private var walletList = listOf<Wallet>()
    private var selectedWalletIdFromEdit: Int = 0

    private var isExpense: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        setupListeners()
    }

    private fun init(view: View) {
        btnIncome = view.findViewById(R.id.btnIncome)
        btnExpense = view.findViewById(R.id.btnExpense)
        edtAmount = view.findViewById(R.id.edtAmount)
        radioGroupIncome = view.findViewById(R.id.radioGroupIncome)
        radioGroupExpense = view.findViewById(R.id.radioGroupExpense)
        edtNote = view.findViewById(R.id.edtNote)
        btnSave = view.findViewById(R.id.btnSave)
        tvDate = view.findViewById(R.id.tvDate)

        spinnerWallet = view.findViewById(R.id.spinnerWallet) // 🔥 เชื่อม Spinner

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
                edtAmount.setText(if(amountVal % 1.0 == 0.0) amountVal.toInt().toString() else amountVal.toString())

                edtNote.setText(bundle.getString("note", ""))
                selectedWalletIdFromEdit = bundle.getInt("walletId", 0) // 🔥 รับค่ากระเป๋าเดิมตอนกดแก้ไข

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

        loadWallets() // 🔥 เรียกใช้งานโหลดกระเป๋าเงิน
    }

    // 🔥 ฟังก์ชันโหลดและสร้างกระเป๋าเงิน
    private fun loadWallets() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            var wallets = db.walletDao().getAllWallets()

            // ถ้าไม่มีกระเป๋าเลย (เปิดแอปครั้งแรก) ให้สร้างขึ้นมา 3 ใบ
            if (wallets.isEmpty()) {
                db.walletDao().insertWallet(Wallet(name = "เงินสด", type = 0, balance = 0.0))
                db.walletDao().insertWallet(Wallet(name = "บัญชีธนาคาร", type = 0, balance = 0.0))
                db.walletDao().insertWallet(Wallet(name = "บัตรเครดิต", type = 1, balance = 0.0)) // type 1 คือหนี้
                wallets = db.walletDao().getAllWallets()
            }

            walletList = wallets

            withContext(Dispatchers.Main) {
                val walletNames = walletList.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
                spinnerWallet.adapter = adapter

                // ถ้าเป็นการกดแก้ไขรายการ ให้เซ็ต Spinner ไปที่กระเป๋าที่เคยเลือกไว้
                if (selectedWalletIdFromEdit != 0) {
                    val selectedIndex = walletList.indexOfFirst { it.id == selectedWalletIdFromEdit }
                    if (selectedIndex >= 0) {
                        spinnerWallet.setSelection(selectedIndex)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        btnIncome.setOnClickListener {
            isExpense = false
            updateTypeSelection()
        }

        btnExpense.setOnClickListener {
            isExpense = true
            updateTypeSelection()
        }

        btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun updateTypeSelection() {
        if (isExpense) {
            btnExpense.setBackgroundColor(Color.parseColor("#78C2A4"))
            btnExpense.setTextColor(Color.WHITE)
            btnIncome.setBackgroundColor(Color.parseColor("#DDDDDD"))
            btnIncome.setTextColor(Color.BLACK)

            radioGroupExpense.visibility = View.VISIBLE
            radioGroupIncome.visibility = View.GONE
        } else {
            btnIncome.setBackgroundColor(Color.parseColor("#78C2A4"))
            btnIncome.setTextColor(Color.WHITE)
            btnExpense.setBackgroundColor(Color.parseColor("#DDDDDD"))
            btnExpense.setTextColor(Color.BLACK)

            radioGroupIncome.visibility = View.VISIBLE
            radioGroupExpense.visibility = View.GONE
        }
    }

    private fun saveTransaction() {
        val amountText = edtAmount.text.toString()
        if (amountText.isEmpty()) {
            edtAmount.error = "กรุณากรอกจำนวนเงิน"
            return
        }
        val amount = amountText.toDouble()

        val activeRadioGroup = if (isExpense) radioGroupExpense else radioGroupIncome
        val selectedId = activeRadioGroup.checkedRadioButtonId
        var category = "อื่นๆ"
        if (selectedId != -1) {
            val selectedRadioButton = view?.findViewById<RadioButton>(selectedId)
            category = selectedRadioButton?.text.toString() ?: "อื่นๆ"
        }

        val note = edtNote.text.toString()
        val type = if (isExpense) 2 else 1

        // 🔥 ดึง ID ของกระเป๋าที่ถูกเลือก
        val selectedWalletIndex = spinnerWallet.selectedItemPosition
        var targetWalletId = 1
        if (selectedWalletIndex >= 0 && walletList.isNotEmpty()) {
            targetWalletId = walletList[selectedWalletIndex].id
        }

        val transaction = Transaction(
            id = currentTransactionId,
            type = type,
            amount = amount,
            category = category,
            note = note,
            date = selectedDate,
            walletId = targetWalletId // 🔥 ใส่ค่ากระเป๋าเงินลงไป (เส้นแดงจะหายไปแล้ว)
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