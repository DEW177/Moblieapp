package com.example.moblieapp

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var txtBalance: TextView
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView
    private lateinit var pieChart: PieChart

    // 🔥 ตัวแปรสำหรับงบประมาณ
    private lateinit var progressBarBudget: ProgressBar
    private lateinit var tvBudgetStatus: TextView
    private lateinit var btnEditBudget: ImageView

    private var monthlyBudget: Float = 10000f // งบเริ่มต้น 10,000 บาท

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtBalance = view.findViewById(R.id.txtBalance)
        txtIncome = view.findViewById(R.id.txtIncome)
        txtExpense = view.findViewById(R.id.txtExpense)
        pieChart = view.findViewById(R.id.pieChart)

        progressBarBudget = view.findViewById(R.id.progressBarBudget)
        tvBudgetStatus = view.findViewById(R.id.tvBudgetStatus)
        btnEditBudget = view.findViewById(R.id.btnEditBudget)

        // ดึงค่างบประมาณที่เคยบันทึกไว้
        val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        monthlyBudget = prefs.getFloat("MonthlyBudget", 10000f)

        // กดปุ่มดินสอเพื่อแก้ไขงบประมาณ
        btnEditBudget.setOnClickListener {
            showEditBudgetDialog()
        }

        loadBalanceAndChart()
    }

    override fun onResume() {
        super.onResume()
        loadBalanceAndChart()
    }

    private fun showEditBudgetDialog() {
        val editText = EditText(requireContext())
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.setText(monthlyBudget.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("ตั้งงบประมาณรายเดือน")
            .setView(editText)
            .setPositiveButton("บันทึก") { _, _ ->
                val input = editText.text.toString()
                if (input.isNotEmpty()) {
                    monthlyBudget = input.toFloat()

                    // บันทึกค่าลงเครื่อง
                    val prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putFloat("MonthlyBudget", monthlyBudget).apply()

                    Toast.makeText(requireContext(), "บันทึกงบประมาณแล้ว", Toast.LENGTH_SHORT).show()
                    loadBalanceAndChart() // โหลดข้อมูลใหม่เพื่ออัปเดตหลอด
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun loadBalanceAndChart() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val transactions = db.transactionDao().getAllTransactions()

            var totalIncome = 0.0
            var totalExpense = 0.0
            var currentMonthExpense = 0.0f

            // ดึงเดือนและปีปัจจุบันไว้เช็ค
            val calendar = Calendar.getInstance()
            val currentMonth = (calendar.get(Calendar.MONTH) + 1).toString()
            val currentYear = calendar.get(Calendar.YEAR).toString()

            val expenseMap = mutableMapOf<String, Float>()

            for (t in transactions) {
                if (t.type == 1) {
                    totalIncome += t.amount
                } else if (t.type == 2) {
                    totalExpense += t.amount

                    val currentAmount = expenseMap[t.category] ?: 0f
                    expenseMap[t.category] = currentAmount + t.amount.toFloat()

                    // เช็คว่าใช่รายจ่ายของเดือนนี้ไหม
                    val dateParts = t.date.split("/")
                    if (dateParts.size == 3 && dateParts[1] == currentMonth && dateParts[2] == currentYear) {
                        currentMonthExpense += t.amount.toFloat()
                    }
                }
            }

            val balance = totalIncome - totalExpense

            withContext(Dispatchers.Main) {
                txtBalance.text = "${String.format("%,.2f", balance)} THB"
                txtIncome.text = "รายรับ\n+ ${String.format("%,.2f", totalIncome)}"
                txtExpense.text = "รายจ่าย\n- ${String.format("%,.2f", totalExpense)}"

                // 🔥 อัปเดตหลอดงบประมาณ
                updateBudgetUI(currentMonthExpense)

                setupPieChart(expenseMap)
            }
        }
    }

    private fun updateBudgetUI(currentMonthExpense: Float) {
        tvBudgetStatus.text = "ใช้ไป ${String.format("%,.0f", currentMonthExpense)} / ${String.format("%,.0f", monthlyBudget)} THB"

        // คำนวณเปอร์เซ็นต์
        val progressPercent = if (monthlyBudget > 0) ((currentMonthExpense / monthlyBudget) * 100).toInt() else 0
        progressBarBudget.progress = progressPercent

        // ถ้าใช้เงินเกิน 90% ให้หลอดเปลี่ยนเป็นสีแดงเตือน
        if (progressPercent >= 90) {
            progressBarBudget.progressTintList = android.content.res.ColorStateList.valueOf(Color.RED)
            tvBudgetStatus.setTextColor(Color.RED)
        } else {
            progressBarBudget.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#78C2A4"))
            tvBudgetStatus.setTextColor(Color.parseColor("#888888"))
        }
    }

    private fun setupPieChart(expenseMap: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        for ((category, amount) in expenseMap) {
            entries.add(PieEntry(amount, category))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "สัดส่วนรายจ่าย"
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}