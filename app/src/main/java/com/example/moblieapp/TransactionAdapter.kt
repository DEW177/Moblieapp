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
    private var walletMap = mapOf<Int, Wallet>()

    var onDeleteClick: ((Transaction) -> Unit)? = null
    var onItemClick: ((Transaction) -> Unit)? = null

    fun setData(list: List<Transaction>, wallets: List<Wallet>) {
        transactionList = list
        walletMap = wallets.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactionList[position]

        holder.tvNote.text = item.note

        // 🔥 เพิ่มการเช็ค type 4 (โอนออก) และ 5 (รับเข้า) ที่เราจะแยกร่างมา
        val categoryIcon = when {
            item.type == 4 -> "📤"
            item.type == 5 -> "📥"
            item.category == "อาหารและเครื่องดื่ม" -> "🍜"
            item.category == "การเดินทาง" -> "🚗"
            item.category == "ช้อปปิ้ง" -> "🛍️"
            item.category == "เงินเดือน / เงินประจำ" -> "💰"
            item.category == "พาร์ทไทม์ / ฟรีแลนซ์" -> "💻"
            item.category == "โบนัส / รางวัล" -> "🎁"
            else -> "📝"
        }

        holder.tvCategory.text = "$categoryIcon ${item.category}"
        holder.tvDate.text = item.date

        val wallet = walletMap[item.walletId]
        if (wallet != null) {
            if (wallet.type == 0) {
                holder.tvWallet.text = "💰 ${wallet.name} (พื้นฐาน)"
                holder.tvWallet.setTextColor(Color.parseColor("#455A64"))
            } else {
                holder.tvWallet.text = "💳 ${wallet.name} (เครดิต)"
                holder.tvWallet.setTextColor(Color.parseColor("#E57373"))
            }
        } else {
            holder.tvWallet.text = "❓ ไม่พบกระเป๋า"
        }

        // 🔥 ถ้าเป็นรายจ่าย (2) หรือ โอนออก (4) ให้เป็นสีแดงติดลบ
        if (item.type == 2 || item.type == 4) {
            holder.tvAmount.text = "- ${item.amount}"
            holder.tvAmount.setTextColor(Color.RED)
        } else {
            // นอกนั้นเป็นรายรับ (1) หรือ รับโอน (5) ให้เป็นสีเขียวบวก
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
        val tvWallet: TextView = itemView.findViewById(R.id.tvWallet)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnDeleteIcon: ImageView = itemView.findViewById(R.id.btnDeleteIcon)
    }
}