package com.example.moblieapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import android.widget.Button

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnHistoryTop = view.findViewById<Button>(R.id.btnHistoryTop)
        val btnAdd = view.findViewById<Button>(R.id.btnAdd)

        btnHistoryTop.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, HistoryFragment())
                .addToBackStack(null)
                .commit()
        }

        btnAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, AddTransactionFragment())
                .addToBackStack(null)
                .commit()
        }


    }
}
