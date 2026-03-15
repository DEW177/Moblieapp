package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomMenu = findViewById<View>(R.id.bottomMenu)
        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnAddMain = findViewById<Button>(R.id.btnAddMain)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        // 🔥 บังคับเตะออกจากระบบทุกครั้งที่เปิดแอป เพื่อเทสหน้า Login
        FirebaseAuth.getInstance().signOut()

        if (savedInstanceState == null) {
            bottomMenu?.visibility = View.GONE
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, LoginFragment())
                .commit()
        }

        // ตั้งค่าให้ปุ่มกดสลับหน้าได้
        btnHome.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }

        btnAddMain.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, AddTransactionFragment())
                .commit()
        }

        btnHistory.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryFragment())
                .commit()
        }
    }
}