package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    // ประกาศตัวแปรตัวเลข
    private lateinit var txtBalance: TextView
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // เชื่อมปุ่ม
        val btnHistoryTop = view.findViewById<Button>(R.id.btnHistoryTop)
        val btnAdd = view.findViewById<Button>(R.id.btnAdd)

        // เชื่อมตัวเลข
        txtBalance = view.findViewById(R.id.txtBalance)
        txtIncome = view.findViewById(R.id.txtIncome)
        txtExpense = view.findViewById(R.id.txtExpense)

        btnHistoryTop.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }

        // โหลดข้อมูลครั้งแรก
        loadBalance()
    }

    // ให้โหลดข้อมูลใหม่ทุกครั้งที่กลับมาหน้านี้ (เช่น เพิ่งกดเพิ่มรายการเสร็จ)
    override fun onResume() {
        super.onResume()
        loadBalance()
    }

    private fun loadBalance() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val transactions = db.transactionDao().getAllTransactions() // ดึงข้อมูลทั้งหมดมา

            var totalIncome = 0.0
            var totalExpense = 0.0

            // วนลูปเพื่อแยกบวกเลข รายรับ และ รายจ่าย
            for (t in transactions) {
                if (t.type == 1) { // 1 = รายรับ
                    totalIncome += t.amount
                } else if (t.type == 2) { // 2 = รายจ่าย
                    totalExpense += t.amount
                }
            }

            // คำนวณยอดคงเหลือ
            val balance = totalIncome - totalExpense

            // สลับกลับมาทำงานบนหน้าจอ
            withContext(Dispatchers.Main) {
                // String.format("%,.2f", ...) ช่วยใส่ลูกน้ำและทศนิยม 2 ตำแหน่งให้สวยงาม (เช่น 1,500.00)
                txtBalance.text = "${String.format("%,.2f", balance)} THB"
                txtIncome.text = "รายรับ\n+ ${String.format("%,.2f", totalIncome)}"
                txtExpense.text = "รายจ่าย\n- ${String.format("%,.2f", totalExpense)}"
            }
        }
    }
}