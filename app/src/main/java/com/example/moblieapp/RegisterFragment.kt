package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.GONE
        auth = FirebaseAuth.getInstance()

        val edtRegName = view.findViewById<EditText>(R.id.edtRegName) // 🔥 ผูกตัวแปรช่องชื่อ
        val edtRegEmail = view.findViewById<EditText>(R.id.edtRegEmail)
        val edtRegPassword = view.findViewById<EditText>(R.id.edtRegPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = view.findViewById<TextView>(R.id.tvGoToLogin)

        btnRegister.setOnClickListener {
            val name = edtRegName.text.toString().trim() // ดึงค่าชื่อ
            val email = edtRegEmail.text.toString().trim()
            val password = edtRegPassword.text.toString().trim()

            // 🔥 เพิ่มการเช็คว่ากรอกชื่อหรือยัง
            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    val safeContext = context ?: return@addOnCompleteListener

                    if (task.isSuccessful) {
                        // 🔥 ถ่ายอดสร้างบัญชีสำเร็จ ให้ทำการอัปเดตชื่อผู้ใช้เข้าไปในโปรไฟล์
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { profileTask ->
                                btnRegister.isEnabled = true

                                if (profileTask.isSuccessful) {
                                    Toast.makeText(safeContext, "สมัครสมาชิกสำเร็จ!", Toast.LENGTH_SHORT).show()
                                    // กลับไปหน้า Login
                                    parentFragmentManager.beginTransaction()
                                        .replace(R.id.fragmentContainerView, LoginFragment())
                                        .commit()
                                } else {
                                    Toast.makeText(safeContext, "อัปเดตชื่อไม่สำเร็จ แต่สร้างบัญชีแล้ว", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        btnRegister.isEnabled = true
                        Toast.makeText(safeContext, "สมัครไม่สำเร็จ: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}