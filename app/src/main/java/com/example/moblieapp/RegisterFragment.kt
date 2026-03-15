package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().findViewById<View>(R.id.bottomMenu)?.visibility = View.GONE
        auth = FirebaseAuth.getInstance()

        val edtRegEmail = view.findViewById<EditText>(R.id.edtRegEmail)
        val edtRegPassword = view.findViewById<EditText>(R.id.edtRegPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = view.findViewById<TextView>(R.id.tvGoToLogin)

        // เปลี่ยนชื่อตัวแปรปุ่ม (btnRegister) ให้ตรงกับในโค้ดของคุณนะครับ
        btnRegister.setOnClickListener {
            val email = edtRegEmail.text.toString().trim()
            val password = edtRegPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(context, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ล็อกปุ่มกันกดรัว
            btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    // ปลดล็อกปุ่ม
                    btnRegister.isEnabled = true

                    // ดึงค่าหน้าจอแบบปลอดภัย
                    val safeContext = context ?: return@addOnCompleteListener
                    val safeActivity = activity ?: return@addOnCompleteListener

                    if (task.isSuccessful) {
                        Toast.makeText(safeContext, "สมัครสมาชิกสำเร็จ!", Toast.LENGTH_SHORT).show()

                        // กลับไปหน้า Login หลังจากสมัครเสร็จ
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainerView, LoginFragment())
                            .commit()
                    } else {
                        // ดึงข้อความ Error จาก Firebase มาโชว์ จะได้รู้ว่าทำไมถึงสมัครไม่ผ่าน
                        Toast.makeText(safeContext, "สมัครไม่สำเร็จ: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        tvGoToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}