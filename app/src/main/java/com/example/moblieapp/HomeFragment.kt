package com.example.moblieapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast // 🔥 เพิ่ม Import Toast ตรงนี้
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
    private lateinit var lineChart: LineChart
    private lateinit var layoutWallets: LinearLayout
    private lateinit var btnManageWallet: TextView
    private lateinit var btnChartExpense: Button
    private lateinit var btnChartIncome: Button

    private lateinit var btnSpendWeek: Button
    private lateinit var btnSpendMonth: Button
    private lateinit var layoutTopSpending: LinearLayout
    private lateinit var layoutRecentTransactions: LinearLayout
    private lateinit var btnSeeAllRecent: TextView

    private var allTransactions = listOf<Transaction>()
    private var isShowingExpenseChart = true
    private var isSpendingFilterMonth = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.VISIBLE
        txtBalance = view.findViewById(R.id.txtBalance)
        txtIncome = view.findViewById(R.id.txtIncome)
        txtExpense = view.findViewById(R.id.txtExpense)
        lineChart = view.findViewById(R.id.lineChart)
        layoutWallets = view.findViewById(R.id.layoutWallets)
        btnManageWallet = view.findViewById(R.id.btnManageWallet)
        btnChartExpense = view.findViewById(R.id.btnChartExpense)
        btnChartIncome = view.findViewById(R.id.btnChartIncome)

        btnSpendWeek = view.findViewById(R.id.btnSpendWeek)
        btnSpendMonth = view.findViewById(R.id.btnSpendMonth)
        layoutTopSpending = view.findViewById(R.id.layoutTopSpending)
        layoutRecentTransactions = view.findViewById(R.id.layoutRecentTransactions)
        btnSeeAllRecent = view.findViewById(R.id.btnSeeAllRecent)

        btnManageWallet.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, ManageWalletFragment())
                .addToBackStack(null)
                .commit()
        }

        btnSeeAllRecent.setOnClickListener {
            Toast.makeText(requireContext(), "ไปหน้า History", Toast.LENGTH_SHORT).show()
        }

        btnChartExpense.setOnClickListener {
            if (!isShowingExpenseChart) {
                isShowingExpenseChart = true
                updateChartToggleUI()
                setupLineChart()
            }
        }

        btnChartIncome.setOnClickListener {
            if (isShowingExpenseChart) {
                isShowingExpenseChart = false
                updateChartToggleUI()
                setupLineChart()
            }
        }

        btnSpendWeek.setOnClickListener {
            if (isSpendingFilterMonth) {
                isSpendingFilterMonth = false
                updateSpendToggleUI()
                renderTopSpending()
            }
        }

        btnSpendMonth.setOnClickListener {
            if (!isSpendingFilterMonth) {
                isSpendingFilterMonth = true
                updateSpendToggleUI()
                renderTopSpending()
            }
        }

        loadBalanceAndChart()
    }

    override fun onResume() {
        super.onResume()
        loadBalanceAndChart()
    }

    private fun updateChartToggleUI() {
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

    private fun updateSpendToggleUI() {
        if (isSpendingFilterMonth) {
            btnSpendMonth.setBackgroundColor(Color.parseColor("#607D8B"))
            btnSpendMonth.setTextColor(Color.WHITE)
            btnSpendWeek.setBackgroundColor(Color.parseColor("#F0F3F5"))
            btnSpendWeek.setTextColor(Color.parseColor("#888888"))
        } else {
            btnSpendWeek.setBackgroundColor(Color.parseColor("#607D8B"))
            btnSpendWeek.setTextColor(Color.WHITE)
            btnSpendMonth.setBackgroundColor(Color.parseColor("#F0F3F5"))
            btnSpendMonth.setTextColor(Color.parseColor("#888888"))
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
                    1 -> {
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) + t.amount
                        if (walletType == 0) totalIncome += t.amount
                    }
                    2 -> {
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) - t.amount
                        totalExpense += t.amount
                    }
                    3 -> {
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

                setupLineChart()
                renderTopSpending()
                renderRecentTransactions()
            }
        }
    }

    private fun setupLineChart() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val targetType = if (isShowingExpenseChart) 2 else 1
        val dailyAmounts = FloatArray(maxDaysInMonth + 1)

        for (t in allTransactions) {
            if (t.type == targetType) {
                val parts = t.date.split("/")
                if (parts.size == 3) {
                    val day = parts[0].trim().toIntOrNull() ?: 1
                    val month = parts[1].trim().toIntOrNull() ?: 1
                    val year = parts[2].trim().toIntOrNull() ?: 1

                    if (month == currentMonth && year == currentYear && day in 1..maxDaysInMonth) {
                        dailyAmounts[day] += t.amount.toFloat()
                    }
                }
            }
        }

        var cumulativeSum = 0f
        val entries = ArrayList<Entry>()
        entries.add(Entry(1f, dailyAmounts[1]))
        cumulativeSum += dailyAmounts[1]

        for (day in 2..today) {
            cumulativeSum += dailyAmounts[day]
            entries.add(Entry(day.toFloat(), cumulativeSum))
        }

        val dataSet = LineDataSet(entries, "")
        val chartColor = if (isShowingExpenseChart) Color.parseColor("#FF5252") else Color.parseColor("#4CAF50")

        dataSet.color = chartColor
        dataSet.setCircleColor(chartColor)
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 0f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = chartColor
        dataSet.fillAlpha = 50

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.setTouchEnabled(true)
        lineChart.isDragEnabled = true
        lineChart.setScaleEnabled(false)

        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.axisMinimum = 1f
        xAxis.axisMaximum = maxDaysInMonth.toFloat()
        xAxis.setDrawGridLines(true)
        xAxis.gridColor = Color.parseColor("#EEEEEE")
        xAxis.textColor = Color.parseColor("#888888")
        xAxis.labelCount = 5
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                val day = value.toInt()
                return "$day/${String.format("%02d", currentMonth)}"
            }
        }

        val yAxis = lineChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.setDrawGridLines(true)
        yAxis.gridColor = Color.parseColor("#EEEEEE")
        yAxis.textColor = Color.parseColor("#888888")

        lineChart.animateX(1000)
        lineChart.invalidate()
    }

    private fun renderTopSpending() {
        layoutTopSpending.removeAllViews()

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentYear = calendar.get(Calendar.YEAR)
        val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        val categorySum = mutableMapOf<String, Double>()
        var totalSpent = 0.0

        for (t in allTransactions) {
            if (t.type == 2) {
                val parts = t.date.split("/")
                if (parts.size == 3) {
                    val day = parts[0].trim().toIntOrNull() ?: 1
                    val month = parts[1].trim().toIntOrNull() ?: 1
                    val year = parts[2].trim().toIntOrNull() ?: 1

                    var shouldInclude = false

                    if (isSpendingFilterMonth) {
                        if (month == currentMonth && year == currentYear) shouldInclude = true
                    } else {
                        val transCal = Calendar.getInstance()
                        transCal.set(year, month - 1, day)
                        if (transCal.get(Calendar.WEEK_OF_YEAR) == currentWeek && year == currentYear) {
                            shouldInclude = true
                        }
                    }

                    if (shouldInclude) {
                        val current = categorySum[t.category] ?: 0.0
                        categorySum[t.category] = current + t.amount
                        totalSpent += t.amount
                    }
                }
            }
        }

        if (categorySum.isEmpty()) {
            val emptyView = TextView(requireContext())
            emptyView.text = "ไม่มีรายการใช้จ่าย"
            emptyView.gravity = Gravity.CENTER
            emptyView.setPadding(0, 20, 0, 20)
            emptyView.setTextColor(Color.parseColor("#888888")) // 🔥 แก้ไขตรงนี้
            layoutTopSpending.addView(emptyView)
            return
        }

        val sortedList = categorySum.toList().sortedByDescending { it.second }.take(3)

        for ((category, amount) in sortedList) {
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 16, 0, 16)
            row.gravity = Gravity.CENTER_VERTICAL

            val catText = TextView(requireContext())
            catText.text = "$category\n฿ ${String.format("%,.0f", amount)}"
            catText.textSize = 15f
            catText.setTextColor(Color.parseColor("#333333")) // 🔥 แก้ไขตรงนี้
            catText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val pctValue = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
            val pctText = TextView(requireContext())
            pctText.text = "${String.format("%.0f", pctValue)}%"
            pctText.textSize = 16f
            pctText.setTypeface(null, Typeface.BOLD)
            pctText.setTextColor(Color.parseColor("#FF5252")) // 🔥 แก้ไขตรงนี้

            row.addView(catText)
            row.addView(pctText)
            layoutTopSpending.addView(row)

            val divider = View(requireContext())
            divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutTopSpending.addView(divider)
        }
    }

    private fun renderRecentTransactions() {
        layoutRecentTransactions.removeAllViews()

        if (allTransactions.isEmpty()) {
            val emptyView = TextView(requireContext())
            emptyView.text = "ยังไม่มีธุรกรรม"
            emptyView.gravity = Gravity.CENTER
            emptyView.setPadding(0, 20, 0, 20)
            emptyView.setTextColor(Color.parseColor("#888888")) // 🔥 แก้ไขตรงนี้
            layoutRecentTransactions.addView(emptyView)
            return
        }

        val recentList = allTransactions.sortedByDescending { it.id }.take(4)

        for (t in recentList) {
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 16, 0, 16)
            row.gravity = Gravity.CENTER_VERTICAL

            val titleText = TextView(requireContext())

            var displayCategory = t.category
            if (t.type == 3) {
                displayCategory = "🔁 โอนเงิน"
            } else if (!displayCategory.any { Character.isSurrogate(it) || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }) {
                displayCategory = if (t.type == 1) "💰 $displayCategory" else "📝 $displayCategory"
            }

            titleText.text = "$displayCategory\n${t.date}"
            titleText.textSize = 15f
            titleText.setTextColor(Color.parseColor("#333333")) // 🔥 แก้ไขตรงนี้
            titleText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val amountText = TextView(requireContext())
            amountText.textSize = 16f
            amountText.setTypeface(null, Typeface.BOLD)

            when (t.type) {
                1 -> {
                    amountText.text = "+${String.format("%,.0f", t.amount)}"
                    amountText.setTextColor(Color.parseColor("#4CAF50")) // 🔥 แก้ไขตรงนี้
                }
                2 -> {
                    amountText.text = "-${String.format("%,.0f", t.amount)}"
                    amountText.setTextColor(Color.parseColor("#FF5252")) // 🔥 แก้ไขตรงนี้
                }
                3 -> {
                    amountText.text = String.format("%,.0f", t.amount)
                    amountText.setTextColor(Color.parseColor("#26A69A")) // 🔥 แก้ไขตรงนี้
                }
            }

            row.addView(titleText)
            row.addView(amountText)
            layoutRecentTransactions.addView(row)

            val divider = View(requireContext())
            divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"))
            layoutRecentTransactions.addView(divider)
        }
    }
}