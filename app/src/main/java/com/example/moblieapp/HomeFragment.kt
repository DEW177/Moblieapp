package com.example.moblieapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var txtBalance: TextView
    private lateinit var txtIncome: TextView
    private lateinit var txtExpense: TextView
    private lateinit var lineChart: LineChart // 🔥 เปลี่ยนจาก PieChart เป็น LineChart
    private lateinit var layoutWallets: LinearLayout
    private lateinit var btnManageWallet: TextView
    private lateinit var btnChartExpense: Button
    private lateinit var btnChartIncome: Button

    private var allTransactions = listOf<Transaction>()
    private var isShowingExpenseChart = true // ตัวแปรเช็คว่ากำลังดูกราฟฝั่งไหน

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtBalance = view.findViewById(R.id.txtBalance)
        txtIncome = view.findViewById(R.id.txtIncome)
        txtExpense = view.findViewById(R.id.txtExpense)
        lineChart = view.findViewById(R.id.lineChart)
        layoutWallets = view.findViewById(R.id.layoutWallets)
        btnManageWallet = view.findViewById(R.id.btnManageWallet)
        btnChartExpense = view.findViewById(R.id.btnChartExpense)
        btnChartIncome = view.findViewById(R.id.btnChartIncome)

        btnManageWallet.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, ManageWalletFragment())
                .addToBackStack(null)
                .commit()
        }

        // 🔥 สร้างระบบกดสลับกราฟ รายจ่าย - รายรับ
        btnChartExpense.setOnClickListener {
            if (!isShowingExpenseChart) {
                isShowingExpenseChart = true
                updateToggleUI()
                setupLineChart()
            }
        }

        btnChartIncome.setOnClickListener {
            if (isShowingExpenseChart) {
                isShowingExpenseChart = false
                updateToggleUI()
                setupLineChart()
            }
        }

        loadBalanceAndChart()
    }

    override fun onResume() {
        super.onResume()
        loadBalanceAndChart()
    }

    private fun updateToggleUI() {
        if (isShowingExpenseChart) {
            btnChartExpense.setBackgroundColor(Color.parseColor("#FF5252"))
            btnChartExpense.setTextColor(Color.WHITE)
            btnChartIncome.setBackgroundColor(Color.parseColor("#E0E0E0"))
            btnChartIncome.setTextColor(Color.parseColor("#888888"))
        } else {
            btnChartExpense.setBackgroundColor(Color.parseColor("#E0E0E0"))
            btnChartExpense.setTextColor(Color.parseColor("#888888"))
            btnChartIncome.setBackgroundColor(Color.parseColor("#4CAF50"))
            btnChartIncome.setTextColor(Color.WHITE)
        }
    }

    private fun loadBalanceAndChart() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            allTransactions = db.transactionDao().getAllTransactions()
            val wallets = db.walletDao().getAllWallets()

            var totalIncome = 0.0
            var totalExpense = 0.0

            val walletBalances = mutableMapOf<Int, Double>()
            val walletTypes = mutableMapOf<Int, Int>()

            for (w in wallets) {
                walletBalances[w.id] = w.balance
                walletTypes[w.id] = w.type
            }

            for (t in allTransactions) {
                val walletType = walletTypes[t.walletId] ?: 0

                when (t.type) {
                    1 -> { // รายรับ
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) + t.amount
                        if (walletType == 0) totalIncome += t.amount
                    }
                    2 -> { // รายจ่าย
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) - t.amount
                        totalExpense += t.amount
                    }
                    3 -> { // โอนเงิน
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) - t.amount
                        t.toWalletId?.let { toId ->
                            walletBalances[toId] = (walletBalances[toId] ?: 0.0) + t.amount
                        }
                    }
                }
            }

            val netWorth = walletBalances.values.sum()

            withContext(Dispatchers.Main) {
                txtBalance.text = "${String.format("%,.2f", netWorth)} THB"
                txtIncome.text = "รายรับ\n+ ${String.format("%,.2f", totalIncome)}"
                txtExpense.text = "รายจ่าย\n- ${String.format("%,.2f", totalExpense)}"

                layoutWallets.removeAllViews()
                for (w in wallets) {
                    val currentBalance = walletBalances[w.id] ?: 0.0
                    val walletView = TextView(requireContext())

                    if (w.type == 0) {
                        walletView.text = "💰 ${w.name}: ${String.format("%,.2f", currentBalance)} ฿"
                        walletView.setTextColor(Color.parseColor("#455A64"))
                    } else {
                        val debt = if (currentBalance < 0) currentBalance * -1 else 0.0
                        val remainingCredit = w.creditLimit + currentBalance
                        walletView.text = "💳 ${w.name}: รูดไป ${String.format("%,.2f", debt)} ฿\n    (วงเงินคงเหลือ: ${String.format("%,.2f", remainingCredit)} ฿)"
                        walletView.setTextColor(Color.parseColor("#E57373"))
                    }

                    walletView.textSize = 16f
                    walletView.setPadding(0, 0, 0, 16)
                    layoutWallets.addView(walletView)
                }

                // 🔥 เรียกวาดกราฟเส้น
                setupLineChart()
            }
        }
    }

    private fun setupLineChart() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // กำหนด Type ที่ต้องการดึง (2 = รายจ่าย, 1 = รายรับ)
        val targetType = if (isShowingExpenseChart) 2 else 1

        // ตัวแปรเก็บยอดรวมของแต่ละวัน
        val dailyAmounts = FloatArray(maxDaysInMonth + 1)

        for (t in allTransactions) {
            if (t.type == targetType) {
                val parts = t.date.split("/")
                if (parts.size == 3) {
                    val day = parts[0].trim().toIntOrNull() ?: 1
                    val month = parts[1].trim().toIntOrNull() ?: 1
                    val year = parts[2].trim().toIntOrNull() ?: 1

                    // ดึงเฉพาะข้อมูลเดือนปัจจุบัน
                    if (month == currentMonth && year == currentYear) {
                        if (day in 1..maxDaysInMonth) {
                            dailyAmounts[day] += t.amount.toFloat()
                        }
                    }
                }
            }
        }

        // นำยอดรายวันมาบวกทบกันเป็น ยอดสะสม (Cumulative) ลากเส้นไปจนถึงวันปัจจุบัน
        var cumulativeSum = 0f
        val entries = ArrayList<Entry>()

        // จุดเริ่มต้นวันที่ 1
        entries.add(Entry(1f, dailyAmounts[1]))
        cumulativeSum += dailyAmounts[1]

        for (day in 2..today) {
            cumulativeSum += dailyAmounts[day]
            entries.add(Entry(day.toFloat(), cumulativeSum))
        }

        val dataSet = LineDataSet(entries, if (isShowingExpenseChart) "รายจ่ายสะสม" else "รายรับสะสม")

        // 🔥 ตกแต่งหน้าตากราฟ
        val chartColor = if (isShowingExpenseChart) Color.parseColor("#FF5252") else Color.parseColor("#4CAF50")

        dataSet.color = chartColor
        dataSet.setCircleColor(chartColor)
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 0f // ซ่อนตัวเลขบนจุดกราฟให้ดูคลีน
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // ทำให้เส้นโค้งสวยงาม

        // เติมสีใต้กราฟ
        dataSet.setDrawFilled(true)
        dataSet.fillColor = chartColor
        dataSet.fillAlpha = 50 // ความโปร่งใสของสีใต้กราฟ

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false // ปิดแกน Y ขวา
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)

        // ตั้งค่าแกน X ให้โชว์วันที่ 1 จนถึงสิ้นเดือน
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisMinimum = 1f
        xAxis.axisMaximum = maxDaysInMonth.toFloat()
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.parseColor("#EEEEEE")
        xAxis.textColor = Color.parseColor("#888888")
        xAxis.labelCount = 5 // โชว์วันที่ประมาณ 5 จุดไม่ให้เบียดกัน
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                val day = value.toInt()
                return "$day/${String.format("%02d", currentMonth)}" // โชว์เป็น "14/03"
            }
        }

        // ตั้งค่าแกน Y ซ้าย
        val yAxis = lineChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.setDrawGridLines(true)
        yAxis.gridColor = Color.parseColor("#EEEEEE")
        yAxis.textColor = Color.parseColor("#888888")

        lineChart.animateX(1000) // แอนิเมชันวาดกราฟ 1 วินาที
        lineChart.invalidate()
    }
}