package com.example.moblieapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryFragment())
                .commit()
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
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val dbFire = FirebaseFirestore.getInstance()

        dbFire.collection("users").document(userId).collection("wallets")
            .addSnapshotListener { walletSnapshots, wError ->
                if (wError != null || walletSnapshots == null || !isAdded) return@addSnapshotListener

                if (walletSnapshots.isEmpty) {
                    val newWalletRef = dbFire.collection("users").document(userId).collection("wallets").document()
                    newWalletRef.set(hashMapOf(
                        "name" to "เงินสด",
                        "type" to 0,
                        "balance" to 0.0,
                        "creditLimit" to 0.0
                    ))
                    return@addSnapshotListener
                }

                val wallets = mutableListOf<Wallet>()
                for (doc in walletSnapshots) {
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val type = doc.getLong("type")?.toInt() ?: 0
                    val balance = doc.getDouble("balance") ?: 0.0
                    val creditLimit = doc.getDouble("creditLimit") ?: 0.0
                    wallets.add(Wallet(id, name, type, balance, creditLimit))
                }

                dbFire.collection("users").document(userId).collection("transactions")
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

                            // 🔥 เพิ่มการดึงค่า timestamp
                            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L

                            // 🔥 ใส่ timestamp เข้าไปใน Transaction
                            tList.add(Transaction(id, type, amount, category, note, date, walletId, toWalletId, timestamp))
                        }
                        allTransactions = tList

                        var totalIncome = 0.0
                        var totalExpense = 0.0
                        val walletBalances = mutableMapOf<String, Double>()

                        for (w in wallets) {
                            walletBalances[w.id] = w.balance
                        }

                        for (t in allTransactions) {
                            when (t.type) {
                                1 -> {
                                    walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) + t.amount
                                    totalIncome += t.amount
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

                        if (!isAdded) return@addSnapshotListener
                        txtBalance.text = "${String.format("%,.2f", netWorth)} THB"
                        txtIncome.text = "รายรับ\n+ ${String.format("%,.2f", totalIncome)}"
                        txtExpense.text = "รายจ่าย\n- ${String.format("%,.2f", totalExpense)}"

                        layoutWallets.removeAllViews()
                        for (w in wallets) {
                            val currentBalance = walletBalances[w.id] ?: 0.0
                            val walletView = TextView(requireContext())

                            walletView.text = "💰 ${w.name}: ${String.format("%,.2f", currentBalance)} ฿"
                            walletView.setTextColor(Color.parseColor("#455A64"))
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

        val entries = ArrayList<Entry>()
        for (day in 1..today) {
            entries.add(Entry(day.toFloat(), dailyAmounts[day]))
        }

        val dataSet = LineDataSet(entries, "")
        val chartColor = if (isShowingExpenseChart) Color.parseColor("#FF5252") else Color.parseColor("#4CAF50")

        dataSet.color = chartColor
        dataSet.setCircleColor(chartColor)
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 0f
        dataSet.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        dataSet.cubicIntensity = 0.2f
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
            emptyView.setTextColor(Color.parseColor("#888888"))
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
            catText.setTextColor(Color.parseColor("#333333"))
            catText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val pctValue = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
            val pctText = TextView(requireContext())
            pctText.text = "${String.format("%.0f", pctValue)}%"
            pctText.textSize = 16f
            pctText.setTypeface(null, Typeface.BOLD)
            pctText.setTextColor(Color.parseColor("#FF5252"))

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
            emptyView.setTextColor(Color.parseColor("#888888"))
            layoutRecentTransactions.addView(emptyView)
            return
        }

        val sdf = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())
        // 🔥 เรียงตามวันที่ก่อน ถ้าตรงกันให้เรียงตามเวลาที่บันทึก
        val recentList = allTransactions.sortedWith(
            compareByDescending<Transaction> { t ->
                try {
                    sdf.parse(t.date)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }.thenByDescending { it.timestamp }
        ).take(4)

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
            titleText.setTextColor(Color.parseColor("#333333"))
            titleText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val amountText = TextView(requireContext())
            amountText.textSize = 16f
            amountText.setTypeface(null, Typeface.BOLD)

            when (t.type) {
                1 -> {
                    amountText.text = "+${String.format("%,.0f", t.amount)}"
                    amountText.setTextColor(Color.parseColor("#4CAF50"))
                }
                2 -> {
                    amountText.text = "-${String.format("%,.0f", t.amount)}"
                    amountText.setTextColor(Color.parseColor("#FF5252"))
                }
                3 -> {
                    amountText.text = String.format("%,.0f", t.amount)
                    amountText.setTextColor(Color.parseColor("#26A69A"))
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