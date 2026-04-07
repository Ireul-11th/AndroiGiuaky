package com.example.giuaky

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class ProductViewModel : ViewModel() {

    private val dbRef: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("products")

    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products

    private var listener: ValueEventListener? = null

    private val cloudName = "dreyiggih"
    private val uploadPreset = "Gkypic"

    fun startListening() {
        if (listener != null) return

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Product>()
                for (child in snapshot.children) {
                    val item = child.getValue(Product::class.java)
                    if (item != null) list.add(item)
                }
                _products.value = list.reversed()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ProductViewModel", "Firebase error: ${error.message}")
            }
        }

        dbRef.addValueEventListener(listener as ValueEventListener)
    }

    fun uploadImageToCloudinary(
        imageUrl: String,
        product: Product,
        onLoading: (Boolean) -> Unit,
        onResult: (String) -> Unit
    ) {
        onLoading(true)

        if (imageUrl.isBlank()) {
            saveProduct(product, onLoading, onResult)
            return
        }

        // Nếu user đang sửa mà không đổi URL ảnh, khỏi upload lại
        if (imageUrl == product.imageUrl) {
            saveProduct(product, onLoading, onResult)
            return
        }

        CloudinaryRetrofit.api.uploadImage(
            cloudName = cloudName,
            fileUrl = imageUrl,
            uploadPreset = uploadPreset
        ).enqueue(object : Callback<CloudinaryResponse> {
            override fun onResponse(
                call: Call<CloudinaryResponse>,
                response: Response<CloudinaryResponse>
            ) {
                if (!response.isSuccessful) {
                    val err = response.errorBody()?.string()
                    onLoading(false)
                    onResult("Cloudinary lỗi: ${response.code()} - $err")
                    return
                }

                val cloudinaryUrl = response.body()?.secure_url.orEmpty()
                if (cloudinaryUrl.isBlank()) {
                    onLoading(false)
                    onResult("Upload xong nhưng không nhận được URL")
                    return
                }

                saveProduct(
                    product.copy(imageUrl = cloudinaryUrl),
                    onLoading,
                    onResult
                )
            }

            override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
                onLoading(false)
                onResult("Lỗi upload Cloudinary: ${t.message}")
            }
        })
    }

    private fun saveProduct(
        product: Product,
        onLoading: (Boolean) -> Unit,
        onResult: (String) -> Unit
    ) {
        val productId = if (product.id.isBlank()) {
            dbRef.push().key ?: UUID.randomUUID().toString()
        } else {
            product.id
        }

        dbRef.child(productId)
            .setValue(product.copy(id = productId))
            .addOnSuccessListener {
                onLoading(false)
                onResult("Đã lưu sản phẩm")
            }
            .addOnFailureListener { e ->
                onLoading(false)
                onResult("Lỗi lưu Firebase: ${e.message}")
            }
    }

    fun deleteProduct(
        product: Product,
        onLoading: (Boolean) -> Unit,
        onResult: (String) -> Unit
    ) {
        onLoading(true)

        dbRef.child(product.id)
            .removeValue()
            .addOnSuccessListener {
                onLoading(false)
                onResult("Đã xoá sản phẩm")
            }
            .addOnFailureListener { e ->
                onLoading(false)
                onResult("Lỗi xoá dữ liệu: ${e.message}")
            }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { dbRef.removeEventListener(it) }
        listener = null
    }
}