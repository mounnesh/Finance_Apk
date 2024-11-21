@file:Suppress("DEPRECATION")

package com.talhaatif.financeapk

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.talhaatif.financeapk.databinding.ActivitySignUpScreenBinding
import com.talhaatif.financeapk.firebase.Util
import com.talhaatif.financeapk.firebase.Variables.Companion.db
import com.talhaatif.financeapk.firebase.Variables.Companion.auth
import com.talhaatif.financeapk.firebase.Variables.Companion.storageRef
import com.talhaatif.financeapk.firebase.Variables.Companion.displayErrorMessage
import com.talhaatif.financeapk.firebase.Variables.Companion.isEmailValid
import java.io.ByteArrayOutputStream

class SignUpScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpScreenBinding
    private val pickImageData = 1
    private var imgChange = true
    private val utils = Util()
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Loading...")

        // Set up the currency selector
        val currencies = listOf("USD", "EUR", "PKR", "INR", "GBP")
        val adapter = ArrayAdapter(this, R.layout.dropdown_menu_popup_item, currencies)
        (binding.currencySelector as? MaterialAutoCompleteTextView)?.setAdapter(adapter)

        binding.register.setOnClickListener {
            if (binding.email.text!!.isEmpty() || binding.password.text!!.isEmpty() || binding.name.text!!.isEmpty() ||
                binding.conpassword.text!!.isEmpty() || binding.currencySelector.text.isEmpty()) {
                displayErrorMessage("Please enter all fields",this)
            } else if (binding.password.text.toString() != binding.conpassword.text.toString()) {
                displayErrorMessage("Passwords do not match",this)
            } else if (!isEmailValid(binding.email.text.toString())) {
                displayErrorMessage("Please enter a valid email",this)
            } else if (imgChange) {
                displayErrorMessage("Please select an image",this)
            } else {
                signUp()
            }
        }

        binding.login.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        binding.imageView.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, pickImageData)
        }
    }

    private fun signUp() {
        progressDialog.show()
        auth.createUserWithEmailAndPassword(binding.email.text.toString(), binding.password.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    uid?.let { saveUidToFirestore(it) }
                } else {
                    progressDialog.dismiss()
                    val error = task.exception?.message
                    error?.let { displayErrorMessage(it,this) }
                }
            }
    }

    private fun saveUidToFirestore(uid: String) {
        val bitmap = (binding.imageView.drawable).toBitmap()
        val imageUri = getImageUri(bitmap)
        val uploadTask = storageRef.child("users/$uid").putFile(imageUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.child("users/$uid").downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result.toString()
                val user = hashMapOf(
                    "uid" to uid,
                    "name" to binding.name.text.toString(),
                    "email" to binding.email.text.toString(),
                    "currency" to binding.currencySelector.text.toString(),
                    "image" to downloadUri
                )

                // Save user details
                db.collection("users")
                    .document(uid) // using uid as document ID for better management
                    .set(user)
                    .addOnSuccessListener {
                        utils.saveLocalData(this, "currency", binding.currencySelector.text.toString())
                        val budget = hashMapOf(
                            "uid" to uid,
                            "balance" to 0.0,
                            "income" to 0.0,
                            "expense" to 0.0
                        )


                        progressDialog.dismiss()
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()


                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        displayErrorMessage("Error saving UID: ${e.message}", this)
                    }
            } else {
                displayErrorMessage("Error retrieving image URL", this)
                progressDialog.dismiss()
            }
        }.addOnFailureListener { e ->
            displayErrorMessage("Error uploading image: ${e.message}", this)
            progressDialog.dismiss()
        }

    }

    private fun getImageUri(inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageData && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImageUri!!))
            binding.imageView.setImageBitmap(bitmap)
            imgChange = false
        }
    }


}
