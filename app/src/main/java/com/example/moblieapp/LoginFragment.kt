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
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ซ่อนเมนูด้านล่างในหน้า Login
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.GONE

        auth = FirebaseAuth.getInstance()

        val edtLoginEmail = view.findViewById<EditText>(R.id.edtLoginEmail)
        val edtLoginPassword = view.findViewById<EditText>(R.id.edtLoginPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = view.findViewById<TextView>(R.id.tvGoToRegister)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword) // 🔥 ผูกตัวแปรลืมรหัสผ่าน

        // กดปุ่มเข้าสู่ระบบ
        btnLogin.setOnClickListener {
            val email = edtLoginEmail.text.toString().trim()
            val password = edtLoginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกอีเมลและรหัสผ่านให้ครบ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    val safeContext = context ?: return@addOnCompleteListener
                    btnLogin.isEnabled = true

                    if (task.isSuccessful) {
                        Toast.makeText(safeContext, "เข้าสู่ระบบสำเร็จ!", Toast.LENGTH_SHORT).show()

                        // แสดงเมนูด้านล่างและไปหน้า Home
                        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.VISIBLE
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainerView, HomeFragment())
                            .commit()
                    } else {
                        Toast.makeText(safeContext, "อีเมลหรือรหัสผ่านไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // กดปุ่มไปหน้าสมัครสมาชิก
        tvGoToRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        // 🔥 กดปุ่มลืมรหัสผ่าน
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    // ฟังก์ชันแสดงป๊อปอัปส่งอีเมลรีเซ็ตรหัสผ่าน
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("กู้คืนรหัสผ่าน")
        builder.setMessage("กรุณากรอกอีเมลที่ใช้สมัครบัญชี เราจะส่งลิงก์สำหรับตั้งรหัสผ่านใหม่ไปให้ครับ")

        val input = EditText(requireContext())
        input.hint = "example@email.com"
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        val layout = LinearLayout(requireContext())
        layout.setPadding(50, 40, 50, 10)
        layout.addView(input, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        builder.setView(layout)

        builder.setPositiveButton("ส่งลิงก์") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "ส่งลิงก์ไปที่อีเมลแล้ว กรุณาเช็คกล่องจดหมาย (หรือ Junk mail)", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(requireContext(), "กรุณากรอกอีเมลก่อนกดส่ง", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.show()
    }
}