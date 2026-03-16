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

        val inputOldPassword = EditText(requireContext())
        inputOldPassword.hint = "รหัสผ่านปัจจุบัน"
        inputOldPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(inputOldPassword, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val inputNewPassword = EditText(requireContext())
        inputNewPassword.hint = "รหัสผ่านใหม่ (ขั้นต่ำ 6 ตัว)"
        inputNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val newPwdParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        newPwdParams.topMargin = 20
        layout.addView(inputNewPassword, newPwdParams)

        val inputConfirmPassword = EditText(requireContext())
        inputConfirmPassword.hint = "ยืนยันรหัสผ่านใหม่อีกครั้ง"
        inputConfirmPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val confirmPwdParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        confirmPwdParams.topMargin = 20
        layout.addView(inputConfirmPassword, confirmPwdParams)

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

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("⚠️ ยืนยันการลบบัญชี")
        builder.setMessage("คุณแน่ใจหรือไม่ว่าจะลบบัญชีนี้? ข้อมูลทั้งหมดของคุณจะไม่สามารถกู้คืนได้อีก")
        builder.setPositiveButton("ลบทิ้งถาวร") { _, _ ->
            auth.currentUser?.delete()?.addOnSuccessListener {
                Toast.makeText(requireContext(), "ลบบัญชีเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                goToLogin()
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