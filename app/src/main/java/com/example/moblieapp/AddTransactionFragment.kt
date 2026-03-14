package com.example.moblieapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddTransactionFragment : Fragment(R.layout.fragment_add_transaction) {

    private var currentTransactionId: Int = 0
    private lateinit var tvDate: TextView
    private var selectedDate: String = ""
    private lateinit var btnIncome: Button
    private lateinit var btnExpense: Button
    private lateinit var btnTransfer: Button
    private lateinit var edtAmount: EditText
    private lateinit var tvCategorySelector: TextView
    private lateinit var tvCategoryLabel: TextView
    private lateinit var edtNote: EditText
    private lateinit var btnSave: Button
    private lateinit var spinnerWallet: Spinner
    private lateinit var spinnerToWallet: Spinner
    private lateinit var tvToWalletLabel: TextView
    private var walletList = listOf<Wallet>()
    private var selectedWalletIdFromEdit: Int = 0
    private var isExpense: Boolean = true
    private var isTransfer: Boolean = false
    private var selectedCategory: String = ""

    // 🔥 อัปเดตลิสต์รายจ่ายแบบมี [HEADER] เพื่อจัดกลุ่ม
    private val defaultExpenseCategories = listOf(
        "[HEADER]อาหาร",
        "🍜 อาหาร & เครื่องดื่ม",
        "[HEADER]บิล & สาธารณูปโภค",
        "🏠 ค่าเช่า", "💧 ค่าน้ำประปา", "⚡ ค่าไฟฟ้า", "📱 ค่าโทรศัพท์", "🌐 ค่าอินเตอร์เน็ต", "📺 บิลโทรทัศน์", "🧾 บิลค่าสาธารณูปโภคอื่นๆ", "⛽ ค่าน้ำมัน",
        "[HEADER]บ้าน",
        "🛋️ ของใช้ในบ้าน", "🛠️ การบำรุงรักษาบ้าน", "🧹 โฮมเซอร์วิส",
        "[HEADER]ช้อปปิ้ง",
        "🛍️ ช้อปปิ้ง", "🧴 ของใช้ส่วนตัว", "💄 แต่งหน้า",
        "[HEADER]การเดินทาง",
        "🚗 การเดินทาง", "🔧 การบำรุงรักษายานพาหนะ",
        "[HEADER]สุขภาพ",
        "💪 สุขภาพ & ฟิตเนส", "🩺 ตรวจสุขภาพ", "🏋️ ฟิตเนส",
        "[HEADER]ครอบครัว",
        "👨‍👩‍👧‍👦 ครอบครัว",
        "[HEADER]สัตว์เลี้ยง",
        "🐶 สัตว์เลี้ยง",
        "[HEADER]การศึกษา",
        "📚 การศึกษา",
        "[HEADER]บันเทิง",
        "🎬 บันเทิง", "🍿 บริการสตรีมมิ่ง", "🎮 เงินสนุก(เติมเกม ซื้อเกม หรืออื่นๆ)",
        "[HEADER]การเงิน",
        "📈 การลงทุน", "🛡️ ประกันภัย", "💸 จ่ายดอกเบี้ย",
        "[HEADER]การให้",
        "🎁 การให้ & บริจาค",
        "[HEADER]อื่นๆ",
        "📝 ค่าใช้จ่ายอื่นๆ"
    )

    private val defaultIncomeCategories = listOf(
        "💰 เงินเดือน", "📦 รายได้อื่นๆ", "📥 โอนเงินเข้า", "📈 เก็บดอกเบี้ย"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init(view)
        setupListeners()
    }

    private fun init(view: View) {
        btnIncome = view.findViewById(R.id.btnIncome)
        btnExpense = view.findViewById(R.id.btnExpense)
        btnTransfer = view.findViewById(R.id.btnTransfer)
        edtAmount = view.findViewById(R.id.edtAmount)
        tvCategorySelector = view.findViewById(R.id.tvCategorySelector)
        tvCategoryLabel = view.findViewById(R.id.tvCategoryLabel)
        edtNote = view.findViewById(R.id.edtNote)
        btnSave = view.findViewById(R.id.btnSave)
        tvDate = view.findViewById(R.id.tvDate)
        spinnerWallet = view.findViewById(R.id.spinnerWallet)
        spinnerToWallet = view.findViewById(R.id.spinnerToWallet)
        tvToWalletLabel = view.findViewById(R.id.tvToWalletLabel)

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        selectedDate = "$day/${month + 1}/$year"
        tvDate.text = "วันที่: $selectedDate"

        tvDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(requireContext(), { _, y, m, d ->
                selectedDate = "$d/${m + 1}/$y"
                tvDate.text = "วันที่: $selectedDate"
            }, year, month, day)
            datePickerDialog.show()
        }

        tvCategorySelector.setOnClickListener { showCategoryDialog() }

        arguments?.let { bundle ->
            currentTransactionId = bundle.getInt("id", 0)

            if (currentTransactionId != 0) {
                val type = bundle.getInt("type", 2)
                isExpense = (type == 2)
                isTransfer = (type == 3)

                val amountVal = bundle.getDouble("amount", 0.0)
                edtAmount.setText(if (amountVal % 1.0 == 0.0) amountVal.toInt().toString() else amountVal.toString())

                if (isTransfer) {
                    edtAmount.isEnabled = false
                    edtAmount.alpha = 0.6f
                    btnIncome.isEnabled = false
                    btnExpense.isEnabled = false
                    btnTransfer.isEnabled = false
                }

                edtNote.setText(bundle.getString("note", ""))
                selectedWalletIdFromEdit = bundle.getInt("walletId", 0)

                val dateStr = bundle.getString("date", "")
                if (dateStr.isNotEmpty()) {
                    selectedDate = dateStr
                    tvDate.text = "วันที่: $selectedDate"
                }

                btnSave.text = "อัปเดตรายการ"
                val categoryStr = bundle.getString("category", "")
                if (categoryStr.isNotEmpty() && !isTransfer) {
                    selectedCategory = categoryStr
                    tvCategorySelector.text = selectedCategory
                }
            }
        }
        updateTypeSelection()
        loadWallets()
    }

    private fun loadWallets() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            var wallets = db.walletDao().getAllWallets()

            if (wallets.isEmpty()) {
                db.walletDao().insertWallet(Wallet(name = "เงินสด", type = 0, balance = 0.0))
                db.walletDao().insertWallet(Wallet(name = "บัญชีธนาคาร", type = 0, balance = 0.0))
                db.walletDao().insertWallet(Wallet(name = "บัตรเครดิต", type = 1, balance = 0.0))
                wallets = db.walletDao().getAllWallets()
            }
            walletList = wallets

            withContext(Dispatchers.Main) {
                updateWalletSpinner()
                val allWalletNames = walletList.map { if (it.type == 0) "💰 ${it.name}" else "💳 ${it.name}" }
                spinnerToWallet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, allWalletNames)

                if (selectedWalletIdFromEdit != 0) {
                    val filteredWallets = if (isExpense || isTransfer) walletList else walletList.filter { it.type == 0 }
                    val selectedIndex = filteredWallets.indexOfFirst { it.id == selectedWalletIdFromEdit }
                    if (selectedIndex >= 0) spinnerWallet.setSelection(selectedIndex)
                }

                arguments?.let { bundle ->
                    val toWalletId = bundle.getInt("toWalletId", 0)
                    if (isTransfer && toWalletId != 0) {
                        val toIndex = walletList.indexOfFirst { it.id == toWalletId }
                        if (toIndex >= 0) spinnerToWallet.setSelection(toIndex)
                    }
                }
            }
        }
    }

    private fun updateWalletSpinner() {
        if (walletList.isEmpty()) return
        val filteredWallets = if (isExpense || isTransfer) walletList else walletList.filter { it.type == 0 }
        val walletNames = filteredWallets.map { if (it.type == 0) "💰 ${it.name}" else "💳 ${it.name}" }
        spinnerWallet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
    }

    private fun setupListeners() {
        btnIncome.setOnClickListener {
            if (currentTransactionId != 0 && isTransfer) return@setOnClickListener
            isExpense = false
            isTransfer = false
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnExpense.setOnClickListener {
            if (currentTransactionId != 0 && isTransfer) return@setOnClickListener
            isExpense = true
            isTransfer = false
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnTransfer.setOnClickListener {
            if (currentTransactionId != 0 && isTransfer) return@setOnClickListener
            isTransfer = true
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnSave.setOnClickListener { saveTransaction() }
    }

    private fun updateTypeSelection() {
        val activeColor = Color.parseColor("#78C2A4")
        val inactiveColor = Color.parseColor("#DDDDDD")

        btnIncome.setBackgroundColor(inactiveColor)
        btnIncome.setTextColor(Color.BLACK)
        btnExpense.setBackgroundColor(inactiveColor)
        btnExpense.setTextColor(Color.BLACK)
        btnTransfer.setBackgroundColor(inactiveColor)
        btnTransfer.setTextColor(Color.BLACK)

        spinnerToWallet.visibility = View.GONE
        tvToWalletLabel.visibility = View.GONE

        if (isTransfer) {
            btnTransfer.setBackgroundColor(activeColor)
            btnTransfer.setTextColor(Color.WHITE)
            spinnerToWallet.visibility = View.VISIBLE
            tvToWalletLabel.visibility = View.VISIBLE
            tvCategorySelector.visibility = View.GONE
            tvCategoryLabel.visibility = View.GONE
        } else {
            if (isExpense) {
                btnExpense.setBackgroundColor(activeColor)
                btnExpense.setTextColor(Color.WHITE)
                if (selectedCategory.isEmpty() || defaultIncomeCategories.contains(selectedCategory) || getCustomCategories(false).contains(selectedCategory)) {
                    // เลือกรายการแรกที่ไม่ใช่ Header
                    selectedCategory = defaultExpenseCategories.first { !it.startsWith("[HEADER]") }
                }
            } else {
                btnIncome.setBackgroundColor(activeColor)
                btnIncome.setTextColor(Color.WHITE)
                if (selectedCategory.isEmpty() || defaultExpenseCategories.contains(selectedCategory) || getCustomCategories(true).contains(selectedCategory)) {
                    selectedCategory = defaultIncomeCategories.first { !it.startsWith("[HEADER]") }
                }
            }
            tvCategorySelector.visibility = View.VISIBLE
            tvCategoryLabel.visibility = View.VISIBLE
            tvCategorySelector.text = selectedCategory
        }
    }

    private fun getCustomCategories(forExpense: Boolean): MutableList<String> {
        val prefs = requireContext().getSharedPreferences("Categories", Context.MODE_PRIVATE)
        val key = if (forExpense) "custom_expense" else "custom_income"
        return prefs.getStringSet(key, emptySet())?.toMutableList() ?: mutableListOf()
    }

    private fun saveCustomCategory(forExpense: Boolean, category: String) {
        val prefs = requireContext().getSharedPreferences("Categories", Context.MODE_PRIVATE)
        val key = if (forExpense) "custom_expense" else "custom_income"
        val set = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(category)
        prefs.edit().putStringSet(key, set).apply()
    }

    // 🔥 ระบบแยกกลุ่มให้สวยงามตอนเปิด Dialog
    private fun showCategoryDialog() {
        val list = mutableListOf<String>()
        list.addAll(if (isExpense) defaultExpenseCategories else defaultIncomeCategories)

        val customCats = getCustomCategories(isExpense)
        if (customCats.isNotEmpty()) {
            list.add("[HEADER]หมวดหมู่ของฉัน")
            list.addAll(customCats)
        }
        list.add("➕ เพิ่มหมวดหมู่ใหม่...")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isExpense) "เลือกหมวดหมู่รายจ่าย" else "เลือกหมวดหมู่รายรับ")

        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, list) {
            // ปิดการคลิกที่ Header
            override fun isEnabled(position: Int): Boolean {
                return !list[position].startsWith("[HEADER]")
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val text = list[position]

                if (text.startsWith("[HEADER]")) {
                    // จัดหน้าตาให้เหมือนหัวข้อกลุ่ม
                    view.text = text.replace("[HEADER]", "")
                    view.setTextColor(Color.parseColor("#26A69A"))
                    view.textSize = 14f
                    view.setTypeface(null, Typeface.BOLD)
                    view.setBackgroundColor(Color.parseColor("#EAF7F3"))
                    view.setPadding(30, 20, 30, 20)
                } else if (text == "➕ เพิ่มหมวดหมู่ใหม่...") {
                    // ปุ่มเพิ่มหมวดหมู่
                    view.text = text
                    view.setTextColor(Color.parseColor("#1976D2"))
                    view.textSize = 16f
                    view.setTypeface(null, Typeface.BOLD)
                    view.setBackgroundColor(Color.TRANSPARENT)
                    view.setPadding(40, 40, 30, 40)
                } else {
                    // หมวดหมู่ปกติ ให้ขยับเยื้องเข้าไป
                    view.text = text
                    view.setTextColor(Color.parseColor("#333333"))
                    view.textSize = 16f
                    view.setTypeface(null, Typeface.NORMAL)
                    view.setBackgroundColor(Color.TRANSPARENT)
                    view.setPadding(70, 30, 30, 30)
                }
                return view
            }
        }

        builder.setAdapter(adapter) { _, which ->
            val selected = list[which]
            if (selected == "➕ เพิ่มหมวดหมู่ใหม่...") {
                showAddCustomCategoryDialog()
            } else if (!selected.startsWith("[HEADER]")) {
                selectedCategory = selected
                tvCategorySelector.text = selectedCategory
            }
        }
        builder.show()
    }

    private fun showAddCustomCategoryDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("เพิ่มหมวดหมู่ใหม่")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val input = EditText(requireContext())
        input.hint = "ใส่อิโมจิและชื่อ (เช่น ☕ กาแฟ)"
        layout.addView(input)

        builder.setView(layout)
        builder.setPositiveButton("เพิ่ม") { _, _ ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                saveCustomCategory(isExpense, text)
                selectedCategory = text
                tvCategorySelector.text = text
            }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }

    private fun saveTransaction() {
        val amountText = edtAmount.text.toString()
        if (amountText.isEmpty()) {
            edtAmount.error = "กรุณากรอกจำนวนเงิน"
            return
        }
        val amount = amountText.toDouble()
        val filteredWallets = if (isExpense || isTransfer) walletList else walletList.filter { it.type == 0 }
        val selectedWalletIndex = spinnerWallet.selectedItemPosition
        val targetWalletId = if (selectedWalletIndex >= 0 && filteredWallets.isNotEmpty()) filteredWallets[selectedWalletIndex].id else 1

        if (isTransfer) {
            val toWalletIndex = spinnerToWallet.selectedItemPosition
            val toWalletId = if (toWalletIndex >= 0 && walletList.isNotEmpty()) walletList[toWalletIndex].id else 1

            if (targetWalletId == toWalletId) {
                Toast.makeText(requireContext(), "ต้นทางและปลายทางต้องไม่เหมือนกัน", Toast.LENGTH_SHORT).show()
                return
            }

            val transaction = Transaction(
                id = currentTransactionId, type = 3, amount = amount, category = "โอนเงิน",
                note = edtNote.text.toString(), date = selectedDate, walletId = targetWalletId, toWalletId = toWalletId
            )

            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                if (currentTransactionId == 0) db.transactionDao().insertTransaction(transaction)
                else db.transactionDao().updateTransaction(transaction)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), if (currentTransactionId == 0) "โอนเงินเรียบร้อย" else "อัปเดตข้อมูลเรียบร้อย", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            return
        }

        val transaction = Transaction(
            id = currentTransactionId,
            type = if (isExpense) 2 else 1,
            amount = amount,
            category = selectedCategory,
            note = edtNote.text.toString(),
            date = selectedDate,
            walletId = targetWalletId
        )

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            if (currentTransactionId == 0) db.transactionDao().insertTransaction(transaction)
            else db.transactionDao().updateTransaction(transaction)

            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), if (currentTransactionId == 0) "บันทึกเรียบร้อย" else "อัปเดตข้อมูลเรียบร้อย", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}