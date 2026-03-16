package com.example.moblieapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class HistoryItem {
    data class DateHeader(val date: String, val dailyTotal: Double) : HistoryItem()
    data class TransactionItem(val transaction: Transaction) : HistoryItem()
}

class TransactionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemList = listOf<HistoryItem>()
    // 🔥 เปลี่ยนจาก Int เป็น String เพื่อรับ ID จาก Firebase
    private var walletMap = mapOf<String, Wallet>()

    var onDeleteClick: ((Transaction) -> Unit)? = null
    var onItemClick: ((Transaction) -> Unit)? = null

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    fun setData(list: List<HistoryItem>, wallets: List<Wallet>) {
        itemList = list
        walletMap = wallets.associateBy { it.id }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (itemList[position]) {
            is HistoryItem.DateHeader -> TYPE_HEADER
            is HistoryItem.TransactionItem -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
            TransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemList[position]

        if (holder is HeaderViewHolder && item is HistoryItem.DateHeader) {
            holder.tvHeaderDate.text = item.date
            if (item.dailyTotal > 0) {
                holder.tvHeaderTotal.text = "+ ${String.format("%,.2f", item.dailyTotal)}"
                holder.tvHeaderTotal.setTextColor(Color.parseColor("#4CAF50"))
            } else if (item.dailyTotal < 0) {
                holder.tvHeaderTotal.text = "${String.format("%,.2f", item.dailyTotal)}"
                holder.tvHeaderTotal.setTextColor(Color.parseColor("#FF5252"))
            } else {
                holder.tvHeaderTotal.text = "0.00"
                holder.tvHeaderTotal.setTextColor(Color.parseColor("#888888"))
            }

        } else if (holder is TransactionViewHolder && item is HistoryItem.TransactionItem) {
            val transaction = item.transaction
            holder.tvNote.text = transaction.note

            var displayCategory = transaction.category
            if (transaction.type == 4) {
                displayCategory = "📤 $displayCategory"
            } else if (transaction.type == 5) {
                displayCategory = "📥 $displayCategory"
            } else {
                val hasEmoji = displayCategory.any { Character.isSurrogate(it) || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }
                if (!hasEmoji) {
                    val icon = when (transaction.category) {
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
            holder.tvDate.visibility = View.GONE

            val wallet = walletMap[transaction.walletId]
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

            if (transaction.type == 2 || transaction.type == 4) {
                holder.tvAmount.text = "- ${String.format("%,.0f", transaction.amount)}"
                holder.tvAmount.setTextColor(Color.RED)
            } else {
                holder.tvAmount.text = "+ ${String.format("%,.0f", transaction.amount)}"
                holder.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
            }

            holder.btnDeleteIcon.setOnClickListener { onDeleteClick?.invoke(transaction) }
            holder.itemView.setOnClickListener { onItemClick?.invoke(transaction) }
        }
    }

    override fun getItemCount(): Int = itemList.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeaderDate: TextView = itemView.findViewById(R.id.tvHeaderDate)
        val tvHeaderTotal: TextView = itemView.findViewById(R.id.tvHeaderTotal)
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvWallet: TextView = itemView.findViewById(R.id.tvWallet)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnDeleteIcon: ImageView = itemView.findViewById(R.id.btnDeleteIcon)
    }
}