package com.example.moblieapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var txtBalance: TextView
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView
    private lateinit var pieChart: PieChart // 🔥 ตัวแปรกราฟ

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnHistoryTop = view.findViewById<Button>(R.id.btnHistoryTop)
        val btnAdd = view.findViewById<Button>(R.id.btnAdd)

        txtBalance = view.findViewById(R.id.txtBalance)
        txtIncome = view.findViewById(R.id.txtIncome)
        txtExpense = view.findViewById(R.id.txtExpense)
        pieChart = view.findViewById(R.id.pieChart) // 🔥 เชื่อมกราฟ

        btnHistoryTop?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        btnAdd?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }

        loadBalanceAndChart()
    }

    override fun onResume() {
        super.onResume()
        loadBalanceAndChart()
    }

    private fun loadBalanceAndChart() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val transactions = db.transactionDao().getAllTransactions()

            var totalIncome = 0.0
            var totalExpense = 0.0

            // 🔥 สร้าง Map ไว้เก็บยอดรวมของแต่ละ "หมวดหมู่รายจ่าย"
            val expenseMap = mutableMapOf<String, Float>()

            for (t in transactions) {
                if (t.type == 1) {
                    totalIncome += t.amount
                } else if (t.type == 2) {
                    totalExpense += t.amount

                    // 🔥 บวกรวมรายจ่ายแยกตามหมวดหมู่
                    val currentAmount = expenseMap[t.category] ?: 0f
                    expenseMap[t.category] = currentAmount + t.amount.toFloat()
                }
            }

            val balance = totalIncome - totalExpense

            withContext(Dispatchers.Main) {
                // อัปเดตตัวเลขยอดเงิน
                txtBalance.text = "${String.format("%,.2f", balance)} THB"
                txtIncome.text = "รายรับ\n+ ${String.format("%,.2f", totalIncome)}"
                txtExpense.text = "รายจ่าย\n- ${String.format("%,.2f", totalExpense)}"

                // 🔥 เรียกฟังก์ชันวาดกราฟ
                setupPieChart(expenseMap)
            }
        }
    }

    // 🔥 ฟังก์ชันสำหรับวาดกราฟ
    private fun setupPieChart(expenseMap: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()

        // ดึงข้อมูลหมวดหมู่มาใส่กราฟ (ถ้าไม่มีรายจ่ายเลย กราฟจะว่างเปล่า)
        for ((category, amount) in expenseMap) {
            entries.add(PieEntry(amount, category))
        }

        val dataSet = PieDataSet(entries, "หมวดหมู่รายจ่าย")

        // เซ็ตสีให้กราฟ (ใช้สีสำเร็จรูปที่ Library มีให้)
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        pieChart.data = data

        // ปรับแต่งความสวยงาม
        pieChart.description.isEnabled = false // ปิดข้อความ Description เล็กๆ
        pieChart.centerText = "สัดส่วนรายจ่าย"
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000) // ให้กราฟค่อยๆ เด้งขึ้นมา 1 วินาที
        pieChart.invalidate() // สั่งให้วาดกราฟใหม่
    }
}