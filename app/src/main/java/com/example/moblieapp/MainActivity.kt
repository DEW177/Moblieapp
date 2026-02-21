package com.example.moblieapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, WelcomeFragment())
                .commit()
        }


        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnAdd = findViewById<Button>(R.id.btnAddMain)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        btnHome.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HomeFragment())
                .commit()
        }

        btnAdd.setOnClickListener {
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
