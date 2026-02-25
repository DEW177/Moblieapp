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

    // ประกาศตัวแปร View
    private var currentTransactionId: Int = 0 // ถ้าเป็น 0 แปลว่าเพิ่มใหม่, ถ้ามีเลขแปลว่าแก้ไข
    private lateinit var tvDate: TextView
    private var selectedDate: String = "" // เก็บค่าวันที่ที่เลือก
    private lateinit var btnIncome: Button
    private lateinit var btnExpense: Button
    private lateinit var edtAmount: EditText
    private lateinit var radioGroupCategory: RadioGroup
    private lateinit var edtNote: EditText
    private lateinit var btnSave: Button

    // ตัวแปรเก็บสถานะว่าเลือก "รายจ่าย" หรือ "รายรับ" (Default คือรายจ่าย)
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
        radioGroupCategory = view.findViewById(R.id.radioGroupCategory)
        edtNote = view.findViewById(R.id.edtNote)
        btnSave = view.findViewById(R.id.btnSave)

        // เชื่อมตัวแปรวันที่
        tvDate = view.findViewById(R.id.tvDate)

        // ตั้งค่าเริ่มต้น: ให้ปุ่มรายจ่ายเป็นสีเข้ม (Active)
        updateTypeSelection()

        // ตั้งค่าวันที่เริ่มต้นให้เป็น "วันนี้"
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        selectedDate = "$day/${month + 1}/$year" // เดือนเริ่มจาก 0 เลยต้อง +1
        tvDate.text = "วันที่: $selectedDate"

        // เมื่อกดปุ่มวันที่ ให้โชว์ Date Picker (ปฏิทิน)
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

        // 🔥 ส่วนใหม่: ตรวจสอบว่ามีข้อมูลส่งมาจากหน้า History หรือไม่ (ถ้ามี = โหมดแก้ไข)
        arguments?.let { bundle ->
            currentTransactionId = bundle.getInt("id", 0)

            if (currentTransactionId != 0) { // เข้าสู่โหมด "แก้ไข"
                // 1. เซ็ตประเภท (รายรับ/รายจ่าย)
                isExpense = (bundle.getInt("type", 2) == 2)
                updateTypeSelection()

                // 2. เซ็ตจำนวนเงิน
                // ลบทศนิยม .0 ออกถ้าเป็นจำนวนเต็ม เพื่อความสวยงาม หรือใส่ตรงๆ ก็ได้
                val amountVal = bundle.getDouble("amount", 0.0)
                edtAmount.setText(if(amountVal % 1.0 == 0.0) amountVal.toInt().toString() else amountVal.toString())

                // 3. เซ็ต Note
                edtNote.setText(bundle.getString("note", ""))

                // 4. เซ็ตวันที่
                val dateStr = bundle.getString("date", "")
                if (dateStr.isNotEmpty()) {
                    selectedDate = dateStr
                    tvDate.text = "วันที่: $selectedDate"
                }

                // 5. เปลี่ยนชื่อปุ่ม
                btnSave.text = "อัปเดตรายการ"

                // 6. เลือกหมวดหมู่เดิมให้ถูกต้อง
                val categoryStr = bundle.getString("category", "")
                for (i in 0 until radioGroupCategory.childCount) {
                    val rb = radioGroupCategory.getChildAt(i) as? RadioButton
                    if (rb?.text.toString() == categoryStr) {
                        rb?.isChecked = true
                        break
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        // กดปุ่มรายรับ
        btnIncome.setOnClickListener {
            isExpense = false
            updateTypeSelection()
        }

        // กดปุ่มรายจ่าย
        btnExpense.setOnClickListener {
            isExpense = true
            updateTypeSelection()
        }

        // กดปุ่มบันทึก
        btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    // ฟังก์ชันเปลี่ยนสีปุ่มตามสถานะที่เลือก
    private fun updateTypeSelection() {
        if (isExpense) {
            // รายจ่าย Active
            btnExpense.setBackgroundColor(Color.parseColor("#78C2A4")) // สีเขียว
            btnExpense.setTextColor(Color.WHITE)

            btnIncome.setBackgroundColor(Color.parseColor("#DDDDDD")) // สีเทา
            btnIncome.setTextColor(Color.BLACK)
        } else {
            // รายรับ Active
            btnIncome.setBackgroundColor(Color.parseColor("#78C2A4")) // สีเขียว
            btnIncome.setTextColor(Color.WHITE)

            btnExpense.setBackgroundColor(Color.parseColor("#DDDDDD")) // สีเทา
            btnExpense.setTextColor(Color.BLACK)
        }
    }

    private fun saveTransaction() {
        // 1. ดึงค่าจาก EditText
        val amountText = edtAmount.text.toString()
        if (amountText.isEmpty()) {
            edtAmount.error = "กรุณากรอกจำนวนเงิน"
            return
        }
        val amount = amountText.toDouble()

        // 2. ดึงค่าหมวดหมู่
        val selectedId = radioGroupCategory.checkedRadioButtonId
        var category = "อื่นๆ"
        if (selectedId != -1) {
            val selectedRadioButton = view?.findViewById<RadioButton>(selectedId)
            category = selectedRadioButton?.text.toString() ?: "อื่นๆ"
        }

        // 3. ดึง Note
        val note = edtNote.text.toString()

        // 4. กำหนดประเภท (1=รายรับ, 2=รายจ่าย)
        val type = if (isExpense) 2 else 1

        // 5. สร้าง Object เตรียมบันทึก
        // 🔥 สำคัญ: ต้องใส่ id = currentTransactionId เพื่อให้รู้ว่าควรอัปเดตอันไหน
        val transaction = Transaction(
            id = currentTransactionId, // ถ้าเป็น 0 คือเพิ่มใหม่, ถ้ามีค่าคืออัปเดต
            type = type,
            amount = amount,
            category = category,
            note = note,
            date = selectedDate
        )

        // 6. *** บันทึกลง Database (ต้องทำใน Background Thread) ***
        val db = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch(Dispatchers.IO) {

            // 🔥 เช็คว่าจะ Insert (เพิ่ม) หรือ Update (แก้ไข)
            if (currentTransactionId == 0) {
                db.transactionDao().insertTransaction(transaction)
            } else {
                db.transactionDao().updateTransaction(transaction)
            }

            // เมื่อเสร็จแล้ว ให้กลับมาทำงานที่ Main Thread เพื่อแจ้งเตือนและปิดหน้า
            withContext(Dispatchers.Main) {
                val message = if (currentTransactionId == 0) "บันทึกข้อมูลเรียบร้อย" else "อัปเดตข้อมูลเรียบร้อย"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // ปิดหน้ากลับไป
            }
        }
    }
}