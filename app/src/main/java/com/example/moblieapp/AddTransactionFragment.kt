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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddTransactionFragment : Fragment(R.layout.fragment_add_transaction) {

    private var currentTransactionId: String = ""
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
    private var selectedWalletIdFromEdit: String = ""
    private var isExpense: Boolean = true
    private var isTransfer: Boolean = false
    private var selectedCategory: String = ""

    private val defaultExpenseCategories = listOf(
        "[HEADER]อาหาร", "🍜 อาหาร & เครื่องดื่ม",
        "[HEADER]บิล & สาธารณูปโภค", "🏠 ค่าเช่า", "💧 ค่าน้ำประปา", "⚡ ค่าไฟฟ้า", "📱 ค่าโทรศัพท์", "🌐 ค่าอินเตอร์เน็ต", "📺 บิลโทรทัศน์", "🧾 บิลค่าสาธารณูปโภคอื่นๆ", "⛽ ค่าน้ำมัน",
        "[HEADER]บ้าน", "🛋️ ของใช้ในบ้าน", "🛠️ การบำรุงรักษาบ้าน", "🧹 โฮมเซอร์วิส",
        "[HEADER]ช้อปปิ้ง", "🛍️ ช้อปปิ้ง", "🧴 ของใช้ส่วนตัว", "💄 แต่งหน้า",
        "[HEADER]การเดินทาง", "🚗 การเดินทาง", "🔧 การบำรุงรักษายานพาหนะ",
        "[HEADER]สุขภาพ", "💪 สุขภาพ & ฟิตเนส", "🩺 ตรวจสุขภาพ", "🏋️ ฟิตเนส",
        "[HEADER]ครอบครัว", "👨‍👩‍👧‍👦 ครอบครัว",
        "[HEADER]สัตว์เลี้ยง", "🐶 สัตว์เลี้ยง",
        "[HEADER]การศึกษา", "📚 การศึกษา",
        "[HEADER]บันเทิง", "🎬 บันเทิง", "🍿 บริการสตรีมมิ่ง", "🎮 เงินสนุก(เติมเกม ซื้อเกม หรืออื่นๆ)",
        "[HEADER]การเงิน", "📈 การลงทุน", "🛡️ ประกันภัย", "💸 จ่ายดอกเบี้ย",
        "[HEADER]การให้", "🎁 การให้ & บริจาค",
        "[HEADER]อื่นๆ", "📝 ค่าใช้จ่ายอื่นๆ"
    )

    private val defaultIncomeCategories = listOf(
        "💰 เงินเดือน", "📦 รายได้อื่นๆ", "📥 โอนเงินเข้า", "📈 เก็บดอกเบี้ย"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.VISIBLE
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
            currentTransactionId = bundle.getString("id", "")

            if (currentTransactionId.isNotEmpty()) {
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
                selectedWalletIdFromEdit = bundle.getString("walletId", "")

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
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).collection("wallets")
            .get()
            .addOnSuccessListener { snapshots ->
                val wallets = mutableListOf<Wallet>()
                for (doc in snapshots) {
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val type = doc.getLong("type")?.toInt() ?: 0
                    val balance = doc.getDouble("balance") ?: 0.0
                    val creditLimit = doc.getDouble("creditLimit") ?: 0.0
                    wallets.add(Wallet(id, name, type, balance, creditLimit))
                }

                walletList = wallets
                updateWalletSpinner()

                val allWalletNames = walletList.map { "💰 ${it.name}" }
                spinnerToWallet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, allWalletNames)

                if (selectedWalletIdFromEdit.isNotEmpty()) {
                    val selectedIndex = walletList.indexOfFirst { it.id == selectedWalletIdFromEdit }
                    if (selectedIndex >= 0) spinnerWallet.setSelection(selectedIndex)
                }

                arguments?.let { bundle ->
                    val toWalletId = bundle.getString("toWalletId", "")
                    if (isTransfer && toWalletId.isNotEmpty()) {
                        val toIndex = walletList.indexOfFirst { it.id == toWalletId }
                        if (toIndex >= 0) spinnerToWallet.setSelection(toIndex)
                    }
                }
            }
    }

    private fun updateWalletSpinner() {
        if (walletList.isEmpty()) return
        val walletNames = walletList.map { "💰 ${it.name}" }
        spinnerWallet.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, walletNames)
    }

    private fun setupListeners() {
        btnIncome.setOnClickListener {
            if (currentTransactionId.isNotEmpty() && isTransfer) return@setOnClickListener
            isExpense = false
            isTransfer = false
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnExpense.setOnClickListener {
            if (currentTransactionId.isNotEmpty() && isTransfer) return@setOnClickListener
            isExpense = true
            isTransfer = false
            updateTypeSelection()
            updateWalletSpinner()
        }

        btnTransfer.setOnClickListener {
            if (currentTransactionId.isNotEmpty() && isTransfer) return@setOnClickListener
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

    private fun deleteCustomCategory(forExpense: Boolean, category: String) {
        val prefs = requireContext().getSharedPreferences("Categories", Context.MODE_PRIVATE)
        val key = if (forExpense) "custom_expense" else "custom_income"
        val set = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove(category)
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun showCategoryDialog() {
        val list = mutableListOf<String>()
        list.addAll(if (isExpense) defaultExpenseCategories else defaultIncomeCategories)

        val customCats = getCustomCategories(isExpense)
        if (customCats.isNotEmpty()) {
            list.add("[HEADER]หมวดหมู่ของฉัน (กดค้างเพื่อลบ)")
            list.addAll(customCats)
        }
        list.add("➕ เพิ่มหมวดหมู่ใหม่...")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (isExpense) "เลือกหมวดหมู่รายจ่าย" else "เลือกหมวดหมู่รายรับ")

        val adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, list) {
            override fun isEnabled(position: Int): Boolean {
                return !list[position].startsWith("[HEADER]")
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val text = list[position]

                if (text.startsWith("[HEADER]")) {
                    view.text = text.replace("[HEADER]", "")
                    view.setTextColor(Color.parseColor("#26A69A"))
                    view.textSize = 14f
                    view.setTypeface(null, Typeface.BOLD)
                    view.setBackgroundColor(Color.parseColor("#EAF7F3"))
                    view.setPadding(30, 20, 30, 20)
                } else if (text == "➕ เพิ่มหมวดหมู่ใหม่...") {
                    view.text = text
                    view.setTextColor(Color.parseColor("#1976D2"))
                    view.textSize = 16f
                    view.setTypeface(null, Typeface.BOLD)
                    view.setBackgroundColor(Color.TRANSPARENT)
                    view.setPadding(40, 40, 30, 40)
                } else {
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

        builder.setAdapter(adapter) { dialogInterface, which ->
            val selected = list[which]
            if (selected == "➕ เพิ่มหมวดหมู่ใหม่...") {
                showAddCustomCategoryDialog()
            } else if (!selected.startsWith("[HEADER]")) {
                selectedCategory = selected
                tvCategorySelector.text = selectedCategory
            }
        }

        val dialog = builder.create()
        dialog.show()

        dialog.listView.setOnItemLongClickListener { _, _, position, _ ->
            val selected = list[position]
            val customCategoriesList = getCustomCategories(isExpense)

            if (customCategoriesList.contains(selected)) {
                AlertDialog.Builder(requireContext())
                    .setTitle("ลบหมวดหมู่")
                    .setMessage("คุณต้องการลบหมวดหมู่ '$selected' ใช่หรือไม่?")
                    .setPositiveButton("ลบ") { _, _ ->
                        deleteCustomCategory(isExpense, selected)
                        dialog.dismiss()
                        showCategoryDialog()

                        if (selectedCategory == selected) {
                            selectedCategory = if (isExpense) defaultExpenseCategories.first { !it.startsWith("[HEADER]") } else defaultIncomeCategories[0]
                            tvCategorySelector.text = selectedCategory
                        }
                    }
                    .setNegativeButton("ยกเลิก", null)
                    .show()
                true
            } else if (!selected.startsWith("[HEADER]") && selected != "➕ เพิ่มหมวดหมู่ใหม่...") {
                Toast.makeText(requireContext(), "หมวดหมู่พื้นฐานไม่สามารถลบได้", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
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

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "กรุณาล็อกอินก่อนบันทึกข้อมูล", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val amount = amountText.toDouble()
        val selectedWalletIndex = spinnerWallet.selectedItemPosition
        val targetWalletId = if (selectedWalletIndex >= 0 && walletList.isNotEmpty()) walletList[selectedWalletIndex].id else ""

        btnSave.isEnabled = false

        val db = FirebaseFirestore.getInstance()
        val transactionRef = if (currentTransactionId.isEmpty()) {
            db.collection("users").document(userId).collection("transactions").document()
        } else {
            db.collection("users").document(userId).collection("transactions").document(currentTransactionId)
        }

        if (isTransfer) {
            val toWalletIndex = spinnerToWallet.selectedItemPosition
            val toWalletId = if (toWalletIndex >= 0 && walletList.isNotEmpty()) walletList[toWalletIndex].id else ""

            if (targetWalletId == toWalletId || targetWalletId.isEmpty() || toWalletId.isEmpty()) {
                Toast.makeText(requireContext(), "กรุณาเลือกต้นทางและปลายทางให้ถูกต้องและไม่ซ้ำกัน", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                return
            }

            // 🔥 ลบ timestamp ออกจากก้อนข้อมูลเริ่มต้น
            val transactionData = mutableMapOf<String, Any>(
                "type" to 3,
                "amount" to amount,
                "category" to "โอนเงิน",
                "note" to edtNote.text.toString(),
                "date" to selectedDate,
                "walletId" to targetWalletId,
                "toWalletId" to toWalletId
            )

            // 🔥 แยกเงื่อนไข: ถ้าสร้างใหม่ให้ใส่ timestamp แล้วใช้ set() | ถ้าแก้ของเดิมให้ใช้ update() ข้อมูลเฉยๆ
            val task = if (currentTransactionId.isEmpty()) {
                transactionData["timestamp"] = FieldValue.serverTimestamp()
                transactionRef.set(transactionData)
            } else {
                transactionRef.update(transactionData)
            }

            task.addOnSuccessListener {
                btnSave.isEnabled = true
                val safeContext = context ?: return@addOnSuccessListener
                val msg = if (currentTransactionId.isEmpty()) "โอนเงินเรียบร้อย" else "อัปเดตข้อมูลเรียบร้อย"
                Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    Toast.makeText(context, "ผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            return
        }

        // 🔥 ลบ timestamp ออกจากก้อนข้อมูลเริ่มต้น
        val transactionData = mutableMapOf<String, Any>(
            "type" to if (isExpense) 2 else 1,
            "amount" to amount,
            "category" to selectedCategory,
            "note" to edtNote.text.toString(),
            "date" to selectedDate,
            "walletId" to targetWalletId
        )

        // 🔥 แยกเงื่อนไขสร้างใหม่ (set) กับแก้ไข (update)
        val task = if (currentTransactionId.isEmpty()) {
            transactionData["timestamp"] = FieldValue.serverTimestamp()
            transactionRef.set(transactionData)
        } else {
            transactionRef.update(transactionData)
        }

        task.addOnSuccessListener {
            btnSave.isEnabled = true
            val safeContext = context ?: return@addOnSuccessListener
            val msg = if (currentTransactionId.isEmpty()) "บันทึกเรียบร้อย" else "อัปเดตข้อมูลเรียบร้อย"
            Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                Toast.makeText(context, "ผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}