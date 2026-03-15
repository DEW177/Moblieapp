package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.GONE
        auth = FirebaseAuth.getInstance()

        // ต้องมีคำว่า val นำหน้าเสมอครับ
        val edtEmail = view.findViewById<EditText>(R.id.edtEmail)
        val edtPassword = view.findViewById<EditText>(R.id.edtPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = view.findViewById<TextView>(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            // ใช้ context เฉยๆ แทน requireContext() เพื่อความปลอดภัย
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 ล็อกปุ่มไว้ ป้องกันคนกดรัวๆ ตอนเน็ตกำลังโหลด
            btnLogin.isEnabled = false

            // ลบ requireActivity() ออกไปเลย เพื่อไม่ให้ผูกมัดกับหน้าต่างที่อาจจะถูกปิดไปแล้ว
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    // ปลดล็อกปุ่มให้กลับมากดได้เหมือนเดิม
                    btnLogin.isEnabled = true

                    // 🔥 ดึงค่าหน้าจอแบบปลอดภัย! ถ้าหน้าจอถูกปิดหรือสลับไปแล้ว ให้หยุดทำงานทันที (ไม่เด้งแน่นอน)
                    val safeContext = context ?: return@addOnCompleteListener
                    val safeActivity = activity ?: return@addOnCompleteListener

                    if (task.isSuccessful) {
                        Toast.makeText(safeContext, "เข้าสู่ระบบสำเร็จ!", Toast.LENGTH_SHORT).show()

                        val bottomMenu = safeActivity.findViewById<View>(R.id.bottomMenu)
                        bottomMenu?.visibility = View.VISIBLE

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainerView, HomeFragment())
                            .commit()
                    } else {
                        Toast.makeText(safeContext, "อีเมลหรือรหัสผ่านไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        tvGoToRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}