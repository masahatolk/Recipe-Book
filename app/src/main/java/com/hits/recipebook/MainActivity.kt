package com.hits.recipebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hits.recipebook.ui.theme.RecipeBookTheme

data class UiProduct(val name: String, val category: String, val isVegan: Boolean)
data class UiDish(val name: String, val category: String, val kcal: Double)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeBookTheme {
                RecipeBookApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBookApp() {
    val products = remember {
        listOf(
            UiProduct("Тофу", "Мясной", true),
            UiProduct("Томат", "Овощи", true),
            UiProduct("Курица", "Мясной", false),
        )
    }
    val dishes = remember {
        listOf(
            UiDish("Томатный суп", "Суп", 180.0),
            UiDish("Салат с тофу", "Салат", 220.0),
        )
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var veganOnly by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = currentTab) {
                Tab(selected = currentTab == 0, onClick = { currentTab = 0 }, text = { Text("Продукты") })
                Tab(selected = currentTab == 1, onClick = { currentTab = 1 }, text = { Text("Блюда") })
            }

            FilterChip(
                modifier = Modifier.padding(12.dp),
                selected = veganOnly,
                onClick = { veganOnly = !veganOnly },
                label = { Text("Только веган") }
            )

            if (currentTab == 0) {
                ProductList(
                    products = if (veganOnly) products.filter { it.isVegan } else products,
                    contentPadding = PaddingValues(12.dp)
                )
            } else {
                DishList(dishes = dishes, contentPadding = PaddingValues(12.dp))
            }
        }
    }
}

@Composable
private fun ProductList(products: List<UiProduct>, contentPadding: PaddingValues) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(products) { product ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text("Категория: ${product.category}")
                    Text(if (product.isVegan) "Веган" else "Не веган")
                }
            }
        }
    }
}

@Composable
private fun DishList(dishes: List<UiDish>, contentPadding: PaddingValues) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(dishes) { dish ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(dish.name, style = MaterialTheme.typography.titleMedium)
                    Text("Категория: ${dish.category}")
                    Text("Калорийность на порцию: ${dish.kcal} ккал")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeBookPreview() {
    RecipeBookTheme { RecipeBookApp() }
}
