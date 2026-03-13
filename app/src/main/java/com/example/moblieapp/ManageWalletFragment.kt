package com.example.moblieapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
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
            showWalletDialog(wallet) // กดเพื่อแก้ไข
        }, { wallet ->
            deleteWallet(wallet) // กดเพื่อลบ
        })
        recyclerView.adapter = adapter

        btnAddWallet.setOnClickListener {
            showWalletDialog(null) // กดเพื่อเพิ่มใหม่
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

    // ฟังก์ชันเด้ง Pop-up ให้กรอกชื่อ/แก้ชื่อกระเป๋า
    private fun showWalletDialog(wallet: Wallet?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(if (wallet == null) "เพิ่มกระเป๋าใหม่" else "แก้ไขกระเป๋าเงิน")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val inputName = EditText(requireContext())
        inputName.hint = "ชื่อกระเป๋า (เช่น KBank, บัตรเครดิต BBL)"
        if (wallet != null) inputName.setText(wallet.name)
        layout.addView(inputName)

        val typeOptions = arrayOf("กระเป๋าเงินขั้นพื้นฐาน", "กระเป๋าเงินเครดิต")
        val spinner = Spinner(requireContext())
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, typeOptions)
        spinner.adapter = spinnerAdapter

        // ถ้าเป็นการแก้ไข ให้โชว์ประเภทเดิม
        if (wallet != null) spinner.setSelection(wallet.type)
        layout.addView(spinner)

        builder.setView(layout)

        builder.setPositiveButton("บันทึก") { _, _ ->
            val name = inputName.text.toString()
            val type = spinner.selectedItemPosition
            if (name.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(requireContext())
                    if (wallet == null) {
                        db.walletDao().insertWallet(Wallet(name = name, type = type, balance = 0.0))
                    } else {
                        db.walletDao().updateWallet(wallet.copy(name = name, type = type))
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
        builder.setMessage("คุณต้องการลบกระเป๋า '${wallet.name}' ใช่หรือไม่? (หมายเหตุ: รายการที่ใช้กระเป๋านี้อาจแสดงผลผิดพลาด)")
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