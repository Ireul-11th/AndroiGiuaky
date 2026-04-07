package com.example.giuaky

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.crossfade

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: ProductViewModel = viewModel()
                    ProductScreen(vm = vm)
                }
            }
        }
    }
}

@Composable
fun ProductScreen(vm: ProductViewModel) {
    val context = LocalContext.current
    val products by vm.products.observeAsState(emptyList())

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.startListening()
    }

    fun clearForm() {
        editingProduct = null
        name = ""
        type = ""
        price = ""
        imageUrl = ""
    }

    fun fillForm(product: Product) {
        editingProduct = product
        name = product.name
        type = product.type
        price = product.price
        imageUrl = product.imageUrl
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Dữ liệu sản phẩm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Nhập thông tin sản phẩm và URL ảnh")
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tên sản phẩm") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Loại sản phẩm") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Giá") },
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL ảnh") },
                singleLine = true
            )
        }

        item {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (name.isBlank() || type.isBlank() || price.isBlank()) {
                        Toast.makeText(context, "Nhập đủ tên, loại, giá", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val product = Product(
                        id = editingProduct?.id.orEmpty(),
                        name = name.trim(),
                        type = type.trim(),
                        price = price.trim(),
                        imageUrl = editingProduct?.imageUrl.orEmpty()
                    )

                    vm.uploadImageToCloudinary(
                        imageUrl = imageUrl.trim(),
                        product = product,
                        onLoading = { isLoading -> loading = isLoading },
                        onResult = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (msg == "Đã lưu sản phẩm") clearForm()
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editingProduct == null) "THÊM SẢN PHẨM" else "CẬP NHẬT SẢN PHẨM")
            }
        }

        item {
            OutlinedButton(
                onClick = { clearForm() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("LÀM MỚI")
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            Text(
                text = "Danh sách sản phẩm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(products, key = { product -> product.id }) { product ->
            ProductItem(
                product = product,
                onEdit = { fillForm(product) },
                onDelete = {
                    vm.deleteProduct(
                        product = product,
                        onLoading = { isLoading -> loading = isLoading },
                        onResult = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            clearForm()
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val context = LocalContext.current

            AsyncImage(
                model = coil3.request.ImageRequest.Builder(context)
                    .data(product.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = product.name,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Tên sp: ${product.name}",
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Loại sp: ${product.type}")
                Text(text = "Giá sp: ${product.price}")
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa")
                }
            }
        }
    }
}