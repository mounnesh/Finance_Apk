package com.talhaatif.financeapk

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.google.firebase.firestore.SetOptions
import com.talhaatif.financeapk.databinding.FragmentProfileBinding
import com.talhaatif.financeapk.firebase.Util
import com.talhaatif.financeapk.firebase.Variables.Companion.auth
import com.talhaatif.financeapk.firebase.Variables.Companion.db
import com.talhaatif.financeapk.firebase.Variables.Companion.storageRef
import java.io.ByteArrayOutputStream
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView


class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var progressDialog: ProgressDialog
    private val utils = Util()
    private var imgChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if ( checkAllParameters() ) {
            progressDialog = ProgressDialog(requireContext())
            progressDialog.setMessage("Fetching profile...")
            progressDialog.show()
            if (isAdded && context != null) {
                fetchUserProfile()
            }
            progressDialog.dismiss()
            setupUpdateButton()
            setupLogoutButton()
            setupImagePicker()
        }
    }
    private fun checkAllParameters(): Boolean {
        return isAdded && context != null
    }

    private fun fetchUserProfile() {
        if (isAdded && context!=null) {
        val userId = Util().getLocalData(requireContext(),"uid") ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: ""
                    val currency = document.getString("currency") ?: ""
                    val profilePictureUrl = document.getString("image") ?: ""

                    binding.name.setText(name)
                    val baseCurrencies = listOf("USD", "EUR", "PKR", "INR", "GBP").toMutableList()

                    if (baseCurrencies.contains(currency)) {
                        val index = baseCurrencies.indexOf(currency)
                        baseCurrencies[index] = baseCurrencies[0]
                    }
                    baseCurrencies[0] = currency
                    try {
                        if (context != null) {
                            val adapter = ArrayAdapter(
                                requireContext(),
                                R.layout.dropdown_menu_popup_item,
                                baseCurrencies
                            )
                            val autoCompleteTextView =
                                binding.currencySelector as? AutoCompleteTextView
                            autoCompleteTextView?.setAdapter(adapter)
                            autoCompleteTextView?.setText(currency, false)
                        }
                    }
                    catch(e : Exception){
                        Log.w("ProfileFragment", "Wait as there is loading ", e)

                    }


                    // Load profile picture using Glide
                    if(isAdded && context!=null) {
                        Glide.with(this)
                            .load(profilePictureUrl)
                            .into(binding.imageView)
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded && context != null) {
                    Log.w("ProfileFragment", "Error getting user profile: ", exception)
                }
             //   Log.w("ProfileFragment", "Error getting user profile: ", exception)
            }
    }
}
    private fun setupUpdateButton() {
        if (!isAdded)
            return
        progressDialog.setMessage("Updating...")
        binding.update.setOnClickListener {
            progressDialog.show()

            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val updatedName = binding.name.text.toString()
            val updatedCurrency = binding.currencySelector.text.toString()

            val userUpdates = hashMapOf(
                "name" to updatedName,
                "currency" to updatedCurrency
            )

            if (imgChange) {
                val bitmap = (binding.imageView.drawable).toBitmap()
                val imageUri = getImageUri(bitmap)
                val uploadTask = storageRef.child("users/$userId").putFile(imageUri)

                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.child("users/$userId").downloadUrl
                }.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUri = task.result.toString()
                        userUpdates["image"] = downloadUri

                        utils.saveLocalData(requireContext(),"currency",updatedCurrency)

                        updateUserInFirestore(userId, userUpdates)
                    } else {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "Error updating profile picture", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                updateUserInFirestore(userId, userUpdates)
            }
        }
    }

    private fun updateUserInFirestore(userId: String, userUpdates: Map<String, String>) {
        db.collection("users").document(userId)
            .set(userUpdates, SetOptions.merge())
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                progressDialog.dismiss()
                Log.w("ProfileFragment", "Error updating profile: ", exception)
            }
    }

    private fun setupLogoutButton() {
        if (!isAdded) return
        binding.logout.setOnClickListener {
            auth.signOut()
            utils.saveLocalData(requireContext(),"auth","false")
            utils.saveLocalData(requireContext(), "uid", "-1")
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun setupImagePicker() {
        if (!isAdded) return
        binding.imageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, pickImageData)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImageData && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri = data.data
            val bitmap = BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(selectedImageUri!!))
            binding.imageView.setImageBitmap(bitmap)
            imgChange = true
        }
    }

    private fun getImageUri(inImage: Bitmap): Uri {

        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireActivity().contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    companion object {
        private const val pickImageData = 1001
    }
}