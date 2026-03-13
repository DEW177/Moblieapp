package com.example.moblieapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WalletAdapter(
    private var wallets: List<Wallet>,
    private val onEditClick: (Wallet) -> Unit,
    private val onDeleteClick: (Wallet) -> Unit
) : RecyclerView.Adapter<WalletAdapter.WalletViewHolder>() {

    inner class WalletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvWalletName: TextView = itemView.findViewById(R.id.tvWalletName)
        val tvWalletType: TextView = itemView.findViewById(R.id.tvWalletType)
        val btnEditWallet: ImageView = itemView.findViewById(R.id.btnEditWallet)
        val btnDeleteWallet: ImageView = itemView.findViewById(R.id.btnDeleteWallet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_wallet, parent, false)
        return WalletViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        val wallet = wallets[position]
        holder.tvWalletName.text = wallet.name
        holder.tvWalletType.text = if (wallet.type == 0) "ประเภท: ทรัพย์สิน (บวก)" else "ประเภท: บัตรเครดิต (หนี้)"

        holder.btnEditWallet.setOnClickListener { onEditClick(wallet) }
        holder.btnDeleteWallet.setOnClickListener { onDeleteClick(wallet) }
    }

    override fun getItemCount(): Int = wallets.size

    fun updateData(newWallets: List<Wallet>) {
        wallets = newWallets
        notifyDataSetChanged()
    }
}