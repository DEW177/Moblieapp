package com.example.moblieapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var transactionList = listOf<Transaction>()

    var onDeleteClick: ((Transaction) -> Unit)? = null
    var onItemClick: ((Transaction) -> Unit)? = null

    fun setData(list: List<Transaction>) {
        transactionList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactionList[position]

        holder.tvNote.text = item.note

        // 🔥 เพิ่มเงื่อนไขเช็คหมวดหมู่เพื่อกำหนดไอคอน (Emoji)
        val categoryIcon = when (item.category) {
            "อาหารและเครื่องดื่ม" -> "🍜"
            "การเดินทาง" -> "🚗"
            "ช้อปปิ้ง" -> "🛍️"
            "เงินเดือน / เงินประจำ" -> "💰"
            "พาร์ทไทม์ / ฟรีแลนซ์" -> "💻"
            "โบนัส / รางวัล" -> "🎁"
            else -> "📝" // สำหรับหมวดหมู่ "อื่นๆ" หรือหมวดหมู่ที่ไม่ได้กำหนดไว้
        }

        // นำ Emoji มาแสดงผลคู่กับชื่อหมวดหมู่
        holder.tvCategory.text = "$categoryIcon ${item.category}"

        holder.tvDate.text = item.date

        if (item.type == 2) {
            holder.tvAmount.text = "- ${item.amount}"
            holder.tvAmount.setTextColor(Color.RED)
        } else {
            holder.tvAmount.text = "+ ${item.amount}"
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
        }

        holder.btnDeleteIcon.setOnClickListener {
            onDeleteClick?.invoke(item)
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = transactionList.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnDeleteIcon: ImageView = itemView.findViewById(R.id.btnDeleteIcon)
    }
}