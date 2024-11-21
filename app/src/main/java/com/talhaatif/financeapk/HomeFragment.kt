package com.talhaatif.financeapk

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.talhaatif.financeapk.adapters.TransactionsAdapter
import com.talhaatif.financeapk.databinding.FragmentHomeBinding
import com.talhaatif.financeapk.firebase.Util
import com.talhaatif.financeapk.models.Transaction

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val db = FirebaseFirestore.getInstance()
    private val utils = Util()
    private var userId = ""
    private var currencyType = "PKR"
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    private fun checkAllParameters(): Boolean {
        return isAdded && context != null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (checkAllParameters()) {

            progressDialog = ProgressDialog(requireContext())
            progressDialog.setMessage("Fetching Account Updates!!!")

            userId = utils.getLocalData(requireContext(), "uid")

            binding.fab.setOnClickListener {

                val intent = Intent(requireActivity(), AddTransactions::class.java)
                startActivity(intent)

            }
            updateBudgetViews()

            db.collection("transactions")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener { result ->
                    val transactions = mutableListOf<Transaction>()
                    for (document in result) {
                        transactions.add(document.toObject(Transaction::class.java))
                    }
                    setupRecyclerView(transactions)
                }
                .addOnFailureListener { e ->
                    // Handle error
                }

        }

    }

    private fun setupRecyclerView(transactions: List<Transaction>) {
        if( isAdded && context != null) {
            binding.transactions.layoutManager = LinearLayoutManager(requireContext())
            binding.transactions.adapter = TransactionsAdapter(transactions, requireContext())
        }
    }

    private fun updateBudgetViews() {
        progressDialog.show()

        userId = utils.getLocalData(requireContext(), "uid")
        val users = db.collection("users").document(userId)

        // Fetch the user's currency type first
        users.get().addOnSuccessListener { result ->
            if (result != null) {
                currencyType = result.getString("currency") ?: "PKR"

                // Now fetch the budget data after currencyType has been updated
                fetchBudgetData()
            } else {
                // Handle the case where the document does not exist
                currencyType = "PKR"
                fetchBudgetData()
            }
        }.addOnFailureListener { e ->
            Log.d("Something", "$e")
            Toast.makeText(requireContext(), "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
            currencyType = "PKR" // Default currency
            fetchBudgetData()
        }
    }
    @SuppressLint("SetTextI18n")
    private fun fetchBudgetData() {
        val budgetRef = db.collection("budget").document(userId)

        budgetRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val income = document.getDouble("income") ?: 0.0
                val expense = document.getDouble("expense") ?: 0.0
                val balance = document.getDouble("balance") ?: 0.0

                binding.tvIncomeAmount.text = "${income} $currencyType"
                binding.tvExpenseAmount.text = "${expense} $currencyType"
                binding.tvBalanceAmount.text = "${balance} $currencyType"
                progressDialog.dismiss()
            } else {
                // Handle the case where the document does not exist
                val initialBudget = hashMapOf(
                    "uid" to userId,
                    "balance" to 0.0,
                    "income" to 0.0,
                    "expense" to 0.0
                )

                budgetRef.set(initialBudget).addOnSuccessListener {
                    binding.tvIncomeAmount.text = "0.0 $currencyType"
                    binding.tvExpenseAmount.text = "0.0 $currencyType"
                    binding.tvBalanceAmount.text = "0.0 $currencyType"
                    progressDialog.dismiss()
                }.addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Error creating budget: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener { exception ->
            progressDialog.dismiss()
            Toast.makeText(requireContext(), "Error getting budget data: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }


}
