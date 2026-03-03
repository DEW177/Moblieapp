package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    // 1. ประกาศตัวแปร Global ไว้ด้านบน
    var btnStart: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. เรียกฟังก์ชัน init
        init(view)

        // 3. ตั้งค่าปุ่ม
        btnStart!!.setOnClickListener {

            // 🔥 แสดง bottom menu ก่อนเปลี่ยนหน้ากลับไปหน้า Home
            requireActivity()
                .findViewById<View>(R.id.bottomMenu)
                ?.visibility = View.VISIBLE

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }
    }

    // 🔥 4. เพิ่ม onResume เพื่อสั่งซ่อนเมนูตอนเข้ามาที่หน้า Welcome
    override fun onResume() {
        super.onResume()
        requireActivity()
            .findViewById<View>(R.id.bottomMenu)
            ?.visibility = View.GONE
    }

    // 5. สร้างฟังก์ชัน init
    private fun init(view: View) {
        btnStart = view.findViewById(R.id.btnStart)
    }
}