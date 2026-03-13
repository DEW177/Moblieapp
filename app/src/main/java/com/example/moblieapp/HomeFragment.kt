package com.example.moblieapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
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
    private lateinit var pieChart: PieChart
    private lateinit var layoutWallets: LinearLayout
    private lateinit var btnManageWallet: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtBalance = view.findViewById(R.id.txtBalance)
        txtIncome = view.findViewById(R.id.txtIncome)
        txtExpense = view.findViewById(R.id.txtExpense)
        pieChart = view.findViewById(R.id.pieChart)
        layoutWallets = view.findViewById(R.id.layoutWallets)

        btnManageWallet = view.findViewById(R.id.btnManageWallet)
        btnManageWallet.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, ManageWalletFragment())
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
            val wallets = db.walletDao().getAllWallets()

            var totalIncome = 0.0
            var totalExpense = 0.0
            val expenseMap = mutableMapOf<String, Float>()

            val walletBalances = mutableMapOf<Int, Double>()
            val walletTypes = mutableMapOf<Int, Int>()

            for (w in wallets) {
                walletBalances[w.id] = w.balance
                walletTypes[w.id] = w.type
            }

            for (t in transactions) {
                val walletType = walletTypes[t.walletId] ?: 0

                when (t.type) {
                    1 -> { // รายรับ
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) + t.amount
                        if (walletType == 0) totalIncome += t.amount
                    }
                    2 -> { // รายจ่าย
                        walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) - t.amount
                        totalExpense += t.amount
                        val currentAmount = expenseMap[t.category] ?: 0f
                        expenseMap[t.category] = currentAmount + t.amount.toFloat()
                    }
                    3 -> { // โอนเงิน — ไม่นับรายรับ/รายจ่าย
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

                setupPieChart(expenseMap)
            }
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
        pieChart.legend.isEnabled = false
        pieChart.centerText = "สัดส่วนรายจ่าย"
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}