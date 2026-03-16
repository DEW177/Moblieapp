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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // 🔥 ดึงข้อมูลกระเป๋าเงินจาก Firebase แบบ Real-time
        db.collection("users").document(userId).collection("wallets")
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null || !isAdded) return@addSnapshotListener

                val wallets = mutableListOf<Wallet>()
                for (doc in snapshots) {
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    val type = doc.getLong("type")?.toInt() ?: 0
                    val balance = doc.getDouble("balance") ?: 0.0
                    val creditLimit = doc.getDouble("creditLimit") ?: 0.0

                    wallets.add(Wallet(id, name, type, balance, creditLimit))
                }

                adapter.updateData(wallets)
            }
    }

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
        if (wallet != null) spinner.setSelection(wallet.type)
        layout.addView(spinner)

        val inputCreditLimit = EditText(requireContext())
        inputCreditLimit.hint = "ใส่วงเงินบัตรเครดิต (เช่น 50000)"
        inputCreditLimit.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputCreditLimit.visibility = if (wallet?.type == 1) View.VISIBLE else View.GONE
        if (wallet != null && wallet.type == 1 && wallet.creditLimit > 0) {
            inputCreditLimit.setText(wallet.creditLimit.toString())
        }

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
            val name = inputName.text.toString().trim()
            val type = spinner.selectedItemPosition
            val limitText = inputCreditLimit.text.toString()
            val limit = if (type == 1 && limitText.isNotEmpty()) limitText.toDouble() else 0.0

            if (name.isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                val db = FirebaseFirestore.getInstance()

                // 🔥 เช็คว่าสร้างใหม่หรือแก้ไข
                val walletRef = if (wallet == null || wallet.id.isEmpty()) {
                    db.collection("users").document(userId).collection("wallets").document() // สร้าง ID ใหม่
                } else {
                    db.collection("users").document(userId).collection("wallets").document(wallet.id) // อัปเดต ID เดิม
                }

                val walletData = hashMapOf(
                    "name" to name,
                    "type" to type,
                    "balance" to (wallet?.balance ?: 0.0),
                    "creditLimit" to limit
                )

                // บันทึกขึ้น Firebase
                walletRef.set(walletData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "บันทึกกระเป๋าสำเร็จ", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "ผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
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
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return@setPositiveButton
            val db = FirebaseFirestore.getInstance()

            // 🔥 ลบข้อมูลจาก Firebase
            db.collection("users").document(userId).collection("wallets").document(wallet.id)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "ลบกระเป๋าเรียบร้อย", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }
}