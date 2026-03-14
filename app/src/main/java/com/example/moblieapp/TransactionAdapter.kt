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

        // 🔥 ดักจับการแสดงผลหมวดหมู่ เพื่อไม่ให้ Emoji เบิ้ลถ้ามีอยู่แล้ว
        var displayCategory = item.category
        if (item.type == 4) {
            displayCategory = "📤 $displayCategory"
        } else if (item.type == 5) {
            displayCategory = "📥 $displayCategory"
        } else {
            // เช็คว่ามี Emoji อยู่ในข้อความแล้วหรือยัง (รองรับทั้งหมวดหมู่เก่าและใหม่)
            val hasEmoji = displayCategory.any { Character.isSurrogate(it) || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }
            if (!hasEmoji) {
                val icon = when (item.category) {
                    "อาหารและเครื่องดื่ม" -> "🍜"
                    "การเดินทาง" -> "🚗"
                    "ช้อปปิ้ง" -> "🛍️"
                    "เงินเดือน / เงินประจำ" -> "💰"
                    "พาร์ทไทม์ / ฟรีแลนซ์" -> "💻"
                    "โบนัส / รางวัล" -> "🎁"
                    else -> "📝"
                }
                displayCategory = "$icon $displayCategory"
            }
        }

        holder.tvCategory.text = displayCategory
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

        if (item.type == 2 || item.type == 4) {
            holder.tvAmount.text = "- ${item.amount}"
            holder.tvAmount.setTextColor(Color.RED)
        } else {
            holder.tvAmount.text = "+ ${item.amount}"
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
        }

        holder.btnDeleteIcon.setOnClickListener { onDeleteClick?.invoke(item) }
        holder.itemView.setOnClickListener { onItemClick?.invoke(item) }
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