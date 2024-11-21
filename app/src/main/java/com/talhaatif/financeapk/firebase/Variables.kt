package com.talhaatif.financeapk.firebase


import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class Variables {
    companion object {
        // for dealing with user sessions, authentication details
        val auth = FirebaseAuth.getInstance()
        @SuppressLint("StaticFieldLeak")
        // for dealing with crud on db
        val db = FirebaseFirestore.getInstance()
        // for dealing with files on db storage like user profile image, or video images like that
        val storageRef = FirebaseStorage.getInstance().reference

        fun displayErrorMessage(message: String, context: Context) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun isEmailValid(email: String): Boolean {
            val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            return emailRegex.matches(email)
        }
    }
}