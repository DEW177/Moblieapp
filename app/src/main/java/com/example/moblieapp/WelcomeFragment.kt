package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment

class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    // 1. ประกาศตัวแปร Global ไว้ด้านบน (เหมือน MainMenu)
    var btnStart: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. เรียกฟังก์ชัน init (ต้องส่ง view เข้าไปให้ด้วย)
        init(view)

        // 3. ตั้งค่าปุ่ม (ใช้ !! เหมือน MainMenu เพื่อยืนยันว่าไม่ null)
        btnStart!!.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    // 4. สร้างฟังก์ชัน init
    // ข้อควรระวัง: ใน Fragment ฟังก์ชันนี้ต้องรับ parameter 'view' เข้ามาด้วย
    // เพราะ Fragment ไม่สามารถเรียก findViewById ได้ด้วยตัวเองเหมือน Activity
    private fun init(view: View) {
        btnStart = view.findViewById(R.id.btnStart)
    }
}