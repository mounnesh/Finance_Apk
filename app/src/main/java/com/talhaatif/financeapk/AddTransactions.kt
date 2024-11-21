package com.talhaatif.financeapk

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.talhaatif.financeapk.databinding.ActivityAddTransactionsBinding
import com.talhaatif.financeapk.firebase.Util
import com.talhaatif.financeapk.firebase.Variables.Companion.db
import com.talhaatif.financeapk.firebase.Variables.Companion.auth
import com.talhaatif.financeapk.firebase.Variables.Companion.storageRef
import java.text.SimpleDateFormat
import java.util.*

class AddTransactions : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionsBinding
    private val utils = Util()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Adding Transaction !!!")

        var selectedDate = ""

        // Date picker logic
        binding.datePickerLayout.setOnClickListener {
            val datePicker = DatePickerFragment { year, month, day ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                binding.tvSelectDate.text = selectedDate
            }
            datePicker.show(supportFragmentManager, "datePicker")
        }

        // Save button click logic
        binding.btnSave.setOnClickListener {
            progressDialog.show()
            val currencyType =  utils.getLocalData(this, "currency")
            val amount = binding.etAmount.text.toString().trim() + " " + currencyType
            val selectedTypeId = binding.rgType.checkedRadioButtonId
            val selectedType = findViewById<RadioButton>(selectedTypeId)?.text.toString()

            if (amount.isEmpty() || selectedTypeId == -1 || selectedDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = utils.getLocalData(this, "uid")

            val transaction = hashMapOf(
                "uid" to userId,
                "transAmount" to amount,
                "transType" to selectedType,
                "transDate" to selectedDate
            )

            db.collection("transactions").add(transaction)
                .addOnSuccessListener {

                    updateBudget(userId, selectedType, amount.split(" ")[0].toDouble())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun updateBudget(userId: String, transType: String, amount: Double) {
        val budgetRef = db.collection("budget").document(userId)

        budgetRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (document != null && document.exists()) {
                    // Document exists, proceed with the transaction
                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(budgetRef)

                        val balance = snapshot.getDouble("balance") ?: 0.0
                        val income = snapshot.getDouble("income") ?: 0.0
                        val expense = snapshot.getDouble("expense") ?: 0.0

                        val newBalance = if (transType == "Income") {
                            balance + amount
                        } else {
                            balance - amount
                        }

                        val newIncome = if (transType == "Income") {
                            income + amount
                        } else {
                            income
                        }

                        val newExpense = if (transType == "Expense") {
                            expense + amount
                        } else {
                            expense
                        }

                        transaction.update(budgetRef, "balance", newBalance)
                        transaction.update(budgetRef, "income", newIncome)
                        transaction.update(budgetRef, "expense", newExpense)
                    }.addOnSuccessListener {
                        progressDialog.dismiss()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating budget: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Document does not exist, create it with initial values
                    val initialBudget = hashMapOf(
                        "uid" to userId,
                        "balance" to if (transType == "Income") amount else -amount,
                        "income" to if (transType == "Income") amount else 0.0,
                        "expense" to if (transType == "Expense") amount else 0.0
                    )
                    budgetRef.set(initialBudget).addOnSuccessListener {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error creating budget: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Error fetching budget document: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
