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

            // เตรียมตัวแปรเก็บยอดเงินแต่ละกระเป๋า
            val walletBalances = mutableMapOf<Int, Double>()
            for (w in wallets) {
                walletBalances[w.id] = w.balance // ยอดเริ่มต้น
            }

            // คำนวณรายรับรายจ่าย และยอดเงินในกระเป๋า
            for (t in transactions) {
                if (t.type == 1) {
                    totalIncome += t.amount
                    walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) + t.amount
                } else if (t.type == 2) {
                    totalExpense += t.amount
                    walletBalances[t.walletId] = (walletBalances[t.walletId] ?: 0.0) - t.amount

                    val currentAmount = expenseMap[t.category] ?: 0f
                    expenseMap[t.category] = currentAmount + t.amount.toFloat()
                }
            }

            // คำนวณทรัพย์สินสุทธิ (Net Worth)
            var netWorth = 0.0
            for (w in wallets) {
                netWorth += walletBalances[w.id] ?: 0.0
            }

            withContext(Dispatchers.Main) {
                txtBalance.text = "${String.format("%,.2f", netWorth)} THB"
                txtIncome.text = "รายรับ\n+ ${String.format("%,.2f", totalIncome)}"
                txtExpense.text = "รายจ่าย\n- ${String.format("%,.2f", totalExpense)}"

                // วาดรายการกระเป๋าเงินบนหน้าจอ
                layoutWallets.removeAllViews()
                for (w in wallets) {
                    val currentBalance = walletBalances[w.id] ?: 0.0
                    val walletView = TextView(requireContext())

                    // เพิ่มสัญลักษณ์ให้ดูง่ายว่าอันไหนคือหนี้
                    val prefix = if (w.type == 1) "💳" else "💰"
                    walletView.text = "$prefix ${w.name}: ${String.format("%,.2f", currentBalance)} ฿"

                    walletView.textSize = 16f
                    walletView.setTextColor(Color.parseColor("#455A64"))
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