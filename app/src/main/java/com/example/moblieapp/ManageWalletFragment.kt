package com.example.moblieapp

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManageWalletFragment : Fragment(R.layout.fragment_manage_wallet) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddWallet: Button
    private lateinit var adapter: WalletAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewWallets)
        btnAddWallet = view.findViewById(R.id.btnAddWallet)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = WalletAdapter(emptyList(), { wallet ->
            showWalletDialog(wallet)
        }, { wallet ->
            deleteWallet(wallet)
        })
        recyclerView.adapter = adapter

        btnAddWallet.setOnClickListener {
            showWalletDialog(null)
        }

        loadWallets()
    }

    private fun loadWallets() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val wallets = db.walletDao().getAllWallets()
            withContext(Dispatchers.Main) {
                adapter.updateData(wallets)
            }
        }
    }

    private fun showWalletDialog(wallet: Wallet?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (wallet == null) "เพิ่มกระเป๋าใหม่" else "แก้ไขกระเป๋าเงิน")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // 1. ช่องกรอกชื่อกระเป๋า
        val inputName = EditText(requireContext())
        inputName.hint = "ชื่อกระเป๋า (เช่น KBank, บัตรเครดิต BBL)"
        if (wallet != null) inputName.setText(wallet.name)
        layout.addView(inputName)

        // 2. เลือกประเภทกระเป๋า
        val typeOptions = arrayOf("กระเป๋าเงินขั้นพื้นฐาน", "กระเป๋าเงินเครดิต")
        val spinner = Spinner(requireContext())
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spinner.adapter = spinnerAdapter
        if (wallet != null) spinner.setSelection(wallet.type)
        layout.addView(spinner)

        // 🔥 3. ช่องกรอกวงเงิน (จะซ่อนไว้ก่อน ถ้าเป็นพื้นฐาน)
        val inputCreditLimit = EditText(requireContext())
        inputCreditLimit.hint = "ใส่วงเงินบัตรเครดิต (เช่น 50000)"
        inputCreditLimit.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputCreditLimit.visibility = if (wallet?.type == 1) View.VISIBLE else View.GONE
        if (wallet != null && wallet.type == 1 && wallet.creditLimit > 0) {
            inputCreditLimit.setText(wallet.creditLimit.toString())
        }

        // ตั้งค่าให้โชว์/ซ่อนช่องกรอกวงเงิน เมื่อกดเปลี่ยนประเภท
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 1) {
                    inputCreditLimit.visibility = View.VISIBLE
                } else {
                    inputCreditLimit.visibility = View.GONE
                    inputCreditLimit.text.clear()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        layout.addView(inputCreditLimit)

        builder.setView(layout)

        builder.setPositiveButton("บันทึก") { _, _ ->
            val name = inputName.text.toString()
            val type = spinner.selectedItemPosition

            // ดึงค่าวงเงิน (ถ้าเว้นว่างไว้ให้เป็น 0.0)
            val limitText = inputCreditLimit.text.toString()
            val limit = if (type == 1 && limitText.isNotEmpty()) limitText.toDouble() else 0.0

            if (name.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(requireContext())
                    if (wallet == null) {
                        db.walletDao().insertWallet(Wallet(name = name, type = type, balance = 0.0, creditLimit = limit))
                    } else {
                        db.walletDao().updateWallet(wallet.copy(name = name, type = type, creditLimit = limit))
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                        loadWallets()
                    }
                }
            }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }

    private fun deleteWallet(wallet: Wallet) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("ยืนยันการลบ")
        builder.setMessage("คุณต้องการลบกระเป๋า '${wallet.name}' ใช่หรือไม่?")
        builder.setPositiveButton("ลบเลย") { _, _ ->
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                db.walletDao().deleteWallet(wallet)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "ลบกระเป๋าเรียบร้อย", Toast.LENGTH_SHORT).show()
                    loadWallets()
                }
            }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }
}