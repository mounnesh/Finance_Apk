package com.talhaatif.financeapk.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.talhaatif.financeapk.R
import com.talhaatif.financeapk.databinding.RvTransactionsBinding
import com.talhaatif.financeapk.models.Transaction

class TransactionsAdapter(private val transactions: List<Transaction>, val context :Context) : RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = RvTransactionsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(private val binding: RvTransactionsBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.transactionType.text = transaction.transType
            binding.transactionAmount.text = transaction.transAmount
            binding.transactionDate.text = transaction.transDate
            if (transaction.transType == "Income"){

                binding.transactionView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.resource_import))

            }
            else if (transaction.transType == "Expense"){

                binding.transactionView.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.export))
            }
            // Set image based on transaction type or any other logic
        }
    }
}
