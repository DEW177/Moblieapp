package com.example.moblieapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
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

    // 🔥 เปลี่ยนจาก radioGroupCategory อันเดียว เป็น 2 อัน
    private lateinit var radioGroupIncome: RadioGroup
    private lateinit var radioGroupExpense: RadioGroup

    private lateinit var edtNote: EditText
    private lateinit var btnSave: Button

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

        // 🔥 เชื่อม RadioGroup 2 อัน
        radioGroupIncome = view.findViewById(R.id.radioGroupIncome)
        radioGroupExpense = view.findViewById(R.id.radioGroupExpense)

        edtNote = view.findViewById(R.id.edtNote)
        btnSave = view.findViewById(R.id.btnSave)
        tvDate = view.findViewById(R.id.tvDate)

        updateTypeSelection() // เซ็ตค่าเริ่มต้น

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

                val dateStr = bundle.getString("date", "")
                if (dateStr.isNotEmpty()) {
                    selectedDate = dateStr
                    tvDate.text = "วันที่: $selectedDate"
                }

                btnSave.text = "อัปเดตรายการ"

                // 🔥 เลือกว่าจะวนหาหมวดหมู่เก่าในกลุ่มไหน
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

            // 🔥 แสดงหมวดรายจ่าย ซ่อนหมวดรายรับ
            radioGroupExpense.visibility = View.VISIBLE
            radioGroupIncome.visibility = View.GONE
        } else {
            btnIncome.setBackgroundColor(Color.parseColor("#78C2A4"))
            btnIncome.setTextColor(Color.WHITE)
            btnExpense.setBackgroundColor(Color.parseColor("#DDDDDD"))
            btnExpense.setTextColor(Color.BLACK)

            // 🔥 แสดงหมวดรายรับ ซ่อนหมวดรายจ่าย
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

        // 🔥 ดึงหมวดหมู่จาก RadioGroup ที่กำลังเปิดใช้งานอยู่
        val activeRadioGroup = if (isExpense) radioGroupExpense else radioGroupIncome
        val selectedId = activeRadioGroup.checkedRadioButtonId
        var category = "อื่นๆ"
        if (selectedId != -1) {
            val selectedRadioButton = view?.findViewById<RadioButton>(selectedId)
            category = selectedRadioButton?.text.toString() ?: "อื่นๆ"
        }

        val note = edtNote.text.toString()
        val type = if (isExpense) 2 else 1

        val transaction = Transaction(
            id = currentTransactionId,
            type = type,
            amount = amount,
            category = category,
            note = note,
            date = selectedDate
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