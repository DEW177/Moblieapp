package com.example.moblieapp

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var auth: FirebaseAuth
    private lateinit var tvUserName: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.VISIBLE

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        tvUserName = view.findViewById(R.id.tvUserName)
        val tvUserEmail = view.findViewById<TextView>(R.id.tvUserEmail)

        val btnEditName = view.findViewById<Button>(R.id.btnEditName)
        val btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword)
        val btnResetData = view.findViewById<Button>(R.id.btnResetData)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        val btnDeleteAccount = view.findViewById<Button>(R.id.btnDeleteAccount)

        if (currentUser != null) {
            updateUI()
            tvUserEmail.text = currentUser.email
        } else {
            tvUserName.text = "ไม่พบผู้ใช้"
            tvUserEmail.text = "ไม่พบอีเมล"
        }

        btnEditName.setOnClickListener { showEditNameDialog() }
        btnChangePassword.setOnClickListener { showChangePasswordDialog() }
        btnResetData.setOnClickListener { showResetDataDialog() }
        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "ออกจากระบบแล้ว", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
        btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
    }

    private fun updateUI() {
        val displayName = auth.currentUser?.displayName
        tvUserName.text = if (!displayName.isNullOrEmpty()) displayName else "ผู้ใช้งาน"
    }

    private fun showEditNameDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("แก้ไขชื่อผู้ใช้")

        val input = EditText(requireContext())
        input.hint = "กรอกชื่อใหม่ของคุณ"
        input.setText(auth.currentUser?.displayName)

        val layout = LinearLayout(requireContext())
        layout.setPadding(50, 40, 50, 10)
        layout.addView(input, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        builder.setView(layout)

        builder.setPositiveButton("บันทึก") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(newName).build()
                auth.currentUser?.updateProfile(profileUpdates)
                    ?.addOnSuccessListener {
                        Toast.makeText(requireContext(), "อัปเดตชื่อสำเร็จ", Toast.LENGTH_SHORT).show()
                        updateUI()
                    }
            }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }

    private fun showChangePasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("เปลี่ยนรหัสผ่าน")

        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        fun createPasswordInput(hintText: String): Pair<com.google.android.material.textfield.TextInputLayout, EditText> {
            val til = com.google.android.material.textfield.TextInputLayout(requireContext())
            til.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            val et = com.google.android.material.textfield.TextInputEditText(til.context)
            et.hint = hintText
            et.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            et.maxLines = 1
            et.isSingleLine = true
            til.addView(et)
            return Pair(til, et)
        }

        val (tilOld, inputOldPassword) = createPasswordInput("รหัสผ่านปัจจุบัน")
        layout.addView(tilOld)

        val (tilNew, inputNewPassword) = createPasswordInput("รหัสผ่านใหม่ (ขั้นต่ำ 6 ตัว)")
        val newParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        newParams.topMargin = 20
        layout.addView(tilNew, newParams)

        val (tilConfirm, inputConfirmPassword) = createPasswordInput("ยืนยันรหัสผ่านใหม่อีกครั้ง")
        val confirmParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        confirmParams.topMargin = 20
        layout.addView(tilConfirm, confirmParams)

        builder.setView(layout)
        builder.setPositiveButton("ยืนยัน", null)
        builder.setNegativeButton("ยกเลิก", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val oldPassword = inputOldPassword.text.toString().trim()
            val newPassword = inputNewPassword.text.toString().trim()
            val confirmPassword = inputConfirmPassword.text.toString().trim()
            val user = auth.currentUser

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "กรุณากรอกข้อมูลให้ครบทุกช่อง", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword.length < 6) {
                Toast.makeText(requireContext(), "รหัสผ่านใหม่ต้องมีอย่างน้อย 6 ตัวอักษร", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "รหัสผ่านใหม่ทั้ง 2 ช่องไม่ตรงกัน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (user != null && user.email != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                user.reauthenticate(credential).addOnSuccessListener {
                    user.updatePassword(newPassword).addOnSuccessListener {
                        Toast.makeText(requireContext(), "เปลี่ยนรหัสผ่านสำเร็จ กรุณาล็อกอินใหม่", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        auth.signOut()
                        goToLogin()
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "รหัสผ่านปัจจุบันไม่ถูกต้อง!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showResetDataDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("⚠️ ยืนยันการรีเซ็ตข้อมูล")
        builder.setMessage("คุณแน่ใจหรือไม่ว่าจะลบข้อมูลทั้งหมด? ข้อมูลนี้จะไม่สามารถกู้คืนได้อีก")

        builder.setPositiveButton("ลบข้อมูลทั้งหมด") { _, _ ->
            val userId = auth.currentUser?.uid ?: return@setPositiveButton
            val db = FirebaseFirestore.getInstance()

            // โฟลเดอร์ย่อยที่อยู่ใต้ชื่อผู้ใช้ (ตามในรูปของคุณ)
            val subcollections = listOf("transactions", "wallets")

            for (collectionName in subcollections) {
                db.collection("users").document(userId).collection(collectionName)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            val batch = db.batch()
                            for (document in documents) {
                                batch.delete(document.reference)
                            }
                            batch.commit()
                        }
                    }
            }
            Toast.makeText(requireContext(), "รีเซ็ตข้อมูลเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("⚠️ ยืนยันการลบบัญชี")
        builder.setMessage("คุณแน่ใจหรือไม่ว่าจะลบบัญชีนี้? ข้อมูลทั้งหมดของคุณจะไม่สามารถกู้คืนได้อีก")

        builder.setPositiveButton("ลบทิ้งถาวร") { _, _ ->
            val user = auth.currentUser
            if (user != null) {
                // ลบข้อมูลใน Firestore ก่อนลบบัญชี Auth
                val db = FirebaseFirestore.getInstance()
                db.collection("Transactions").document(user.uid).delete().addOnCompleteListener {
                    user.delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "ลบบัญชีเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                            goToLogin()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "เพื่อความปลอดภัย กรุณาล็อกเอาท์และล็อกอินใหม่ก่อนกดลบบัญชี", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }

    private fun goToLogin() {
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.GONE
        parentFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, LoginFragment()).commit()
    }
}