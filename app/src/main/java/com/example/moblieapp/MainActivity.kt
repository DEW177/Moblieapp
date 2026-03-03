package com.example.moblieapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    lateinit var bottomMenu: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ผูก bottomMenu กับ ID แท็กนอกสุดที่ครอบเมนูไว้
        bottomMenu = findViewById(R.id.bottomMenu)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, WelcomeFragment())
                .commit()


            bottomMenu.visibility = View.GONE
        }

        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnAdd = findViewById<Button>(R.id.btnAddMain)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        btnHome.setOnClickListener {

            bottomMenu.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }

        btnAdd.setOnClickListener {

            bottomMenu.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, AddTransactionFragment())
                .commit()
        }

        btnHistory.setOnClickListener {

            bottomMenu.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryFragment())
                .commit()
        }
    }
}