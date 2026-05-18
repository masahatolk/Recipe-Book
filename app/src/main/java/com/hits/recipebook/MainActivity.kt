package com.hits.recipebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.hits.recipebook.network.DeletionBlockedResponse
import com.hits.recipebook.network.DishUpsertRequest
import com.hits.recipebook.network.ProductUpsertRequest
import com.hits.recipebook.network.RecipeBookApi
import com.hits.recipebook.network.RecipeBookApiFactory
import com.hits.recipebook.ui.theme.RecipeBookTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.Locale

private val TOMSK_ZONE_ID: ZoneId = ZoneId.of("Asia/Tomsk")

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

data class ProductFormState(
    val id: String? = null,
    val name: String = "",
    val photos: List<String> = emptyList(),
    val calories: String = "0",
    val proteins: String = "0",
    val fats: String = "0",
    val carbs: String = "0",
    val composition: String = "",
    val category: ProductCategory = ProductCategory.VEGETABLES,
    val cookingRequirement: CookingRequirement = CookingRequirement.READY_TO_EAT,
    val flags: Set<ExtraFlag> = emptySet(),
)

data class DishFormState(
    val id: String? = null,
    val name: String = "",
    val photos: List<String> = emptyList(),
    val calories: String = "0",
    val proteins: String = "0",
    val fats: String = "0",
    val carbs: String = "0",
    val portionSize: String = "100",
    val category: DishCategory? = null,
    val flags: Set<ExtraFlag> = emptySet(),
    val ingredientGrams: Map<String, String> = emptyMap(),
    val isNutritionManuallyEdited: Boolean = false,
)

private sealed interface DetailScreen {
    data class ProductDetails(val product: Product) : DetailScreen
    data class DishDetails(val dish: Dish) : DetailScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBookApp() {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val products = remember { mutableStateListOf<Product>() }
    val dishes = remember { mutableStateListOf<Dish>() }
    val api = remember { RecipeBookApiFactory.api }

    var tab by remember { mutableIntStateOf(0) }
    var productSearch by remember { mutableStateOf("") }
    var productCategoryFilter by remember { mutableStateOf(setOf<ProductCategory>()) }
    var cookingFilter by remember { mutableStateOf(setOf<CookingRequirement>()) }
    var productFlagsFilter by remember { mutableStateOf(setOf<ExtraFlag>()) }
    var productSort by remember { mutableStateOf(ProductSort.NAME) }
    var isProductSortAscending by remember { mutableStateOf(true) }
    var dishSearch by remember { mutableStateOf("") }
    var dishCategoryFilter by remember { mutableStateOf(setOf<DishCategory>()) }
    var dishFlagsFilter by remember { mutableStateOf(setOf<ExtraFlag>()) }

    var productForm by remember { mutableStateOf(ProductFormState()) }
    var productError by remember { mutableStateOf<String?>(null) }
    var isProductEditorVisible by remember { mutableStateOf(false) }

    var dishForm by remember { mutableStateOf(DishFormState()) }
    var dishError by remember { mutableStateOf<String?>(null) }
    var isDishEditorVisible by remember { mutableStateOf(false) }

    val productById = products.associateBy { it.id }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedDish by remember { mutableStateOf<Dish?>(null) }
    var detailScreen by remember { mutableStateOf<DetailScreen?>(null) }

    val productComparator = when (productSort) {
        ProductSort.NAME -> compareBy<Product> { it.name.lowercase() }
        ProductSort.CALORIES -> compareBy { it.nutritionPer100g.calories }
        ProductSort.PROTEINS -> compareBy { it.nutritionPer100g.proteins }
        ProductSort.FATS -> compareBy { it.nutritionPer100g.fats }
        ProductSort.CARBS -> compareBy { it.nutritionPer100g.carbs }
    }.let { comparator ->
        if (isProductSortAscending) comparator else comparator.reversed()
    }

    val filteredProducts = products
        .filter { productSearch.isBlank() || it.name.contains(productSearch, ignoreCase = true) }
        .filter { productCategoryFilter.isEmpty() || productCategoryFilter.contains(it.category) }
        .filter { cookingFilter.isEmpty() || cookingFilter.contains(it.cookingRequirement) }
        .filter { it.flags.containsAll(productFlagsFilter) }
        .sortedWith(productComparator)

    val filteredDishes = dishes
        .filter { dishSearch.isBlank() || it.name.contains(dishSearch, ignoreCase = true) }
        .filter { dishCategoryFilter.isEmpty() || dishCategoryFilter.contains(it.category) }
        .filter { it.flags.containsAll(dishFlagsFilter) }

    LaunchedEffect(Unit) {
        runCatching {
            api.getProducts().map { it.withNormalizedPhotoUrls() }
        }.onSuccess { loadedProducts ->
            products.clear()
            products.addAll(loadedProducts)
        }.onFailure {
            snackBarHostState.showSnackbar("Не удалось загрузить продукты: ${it.message}")
        }

        runCatching {
            api.getDishes().map { it.withNormalizedPhotoUrls() }
        }.onSuccess { loadedDishes ->
            dishes.clear()
            dishes.addAll(loadedDishes)
        }.onFailure {
            snackBarHostState.showSnackbar("Не удалось загрузить блюда: ${it.message}")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { padding ->
        val isEditorVisible =
            (tab == 0 && isProductEditorVisible) || (tab == 1 && isDishEditorVisible)
        val isNavigationVisible = !isEditorVisible && detailScreen == null
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            if (isNavigationVisible) {
                TabRow(tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Продукты") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Блюда") })
                }
            }
            if (detailScreen != null) {
                when (val currentDetail = detailScreen) {
                    is DetailScreen.ProductDetails -> ProductDetailsScreen(
                        product = currentDetail.product,
                        onBack = { detailScreen = null },
                    )

                    is DetailScreen.DishDetails -> DishDetailsScreen(
                        dish = currentDetail.dish,
                        productsById = productById,
                        onBack = { detailScreen = null },
                    )

                    null -> Unit
                }
            } else if (tab == 0) {
                if (!isProductEditorVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            productForm = ProductFormState()
                            productError = null
                            isProductEditorVisible = true
                        }) { Text("Создать продукт") }
                    }
                }
                if (isProductEditorVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        ProductEditor(
                            api = api,
                            form = productForm,
                            error = productError,
                            onChange = { productForm = it },
                            onUploadFailure = { message ->
                                scope.launch { snackBarHostState.showSnackbar(message) }
                            },
                            onSave = {
                                runCatching {
                                    require(productForm.name.trim().length >= 2) { "Название продукта: минимум 2 символа" }
                                    require(productForm.photos.size <= 5) { "Можно указать не более 5 фото" }
                                    val nutrition = Nutrition(
                                        calories = parseRequiredDouble(
                                            productForm.calories,
                                            "Калорийность"
                                        )
                                            .also { require(it >= 0) { "Калорийность должна быть >= 0" } },
                                        proteins = parseRequiredDouble(
                                            productForm.proteins,
                                            "Белки"
                                        )
                                            .also { require(it in 0.0..100.0) { "Белки должны быть в диапазоне 0..100" } },
                                        fats = parseRequiredDouble(productForm.fats, "Жиры")
                                            .also { require(it in 0.0..100.0) { "Жиры должны быть в диапазоне 0..100" } },
                                        carbs = parseRequiredDouble(productForm.carbs, "Углеводы")
                                            .also { require(it in 0.0..100.0) { "Углеводы должны быть в диапазоне 0..100" } },
                                    )

                                    require(nutrition.proteins + nutrition.fats + nutrition.carbs <= 100.0) {
                                        "Сумма БЖУ на 100 г не может превышать 100"
                                    }
                                    val request = ProductUpsertRequest(
                                        name = productForm.name.trim(),
                                        photos = productForm.photos,
                                        nutritionPer100g = nutrition,
                                        composition = productForm.composition.ifBlank { null },
                                        category = productForm.category,
                                        cookingRequirement = productForm.cookingRequirement,
                                        flags = productForm.flags,
                                    )
                                    scope.launch {
                                        runCatching {
                                            val saved = if (productForm.id == null) {
                                                api.createProduct(request)
                                            } else {
                                                api.updateProduct(productForm.id!!, request)
                                            }.withNormalizedPhotoUrls()
                                            val currentIndex =
                                                products.indexOfFirst { it.id == saved.id }
                                            if (currentIndex >= 0) products[currentIndex] =
                                                saved else products += saved
                                            productForm = ProductFormState()
                                            productError = null
                                            isProductEditorVisible = false
                                        }.onFailure {
                                            productError = it.message ?: "Ошибка сохранения"
                                        }
                                    }
                                }.onFailure { productError = it.message ?: "Ошибка сохранения" }
                            },
                            onCancel = {
                                productForm = ProductFormState()
                                productError = null
                                isProductEditorVisible = false
                            }
                        )
                    }
                }

                if (!isProductEditorVisible) {
                    ProductFilterBlock(
                        search = productSearch,
                        onSearchChange = { productSearch = it },
                        categoryFilter = productCategoryFilter,
                        onCategoryFilterToggle = { category ->
                            productCategoryFilter =
                                if (productCategoryFilter.contains(category)) productCategoryFilter - category
                                else productCategoryFilter + category
                        },
                        onCategoryFilterReset = { productCategoryFilter = emptySet() },
                        cookingFilter = cookingFilter,
                        onCookingFilterToggle = { cooking ->
                            cookingFilter =
                                if (cookingFilter.contains(cooking)) cookingFilter - cooking
                                else cookingFilter + cooking
                        },
                        onCookingFilterReset = { cookingFilter = emptySet() },
                        flagsFilter = productFlagsFilter,
                        onFlagToggle = { flag ->
                            productFlagsFilter =
                                if (productFlagsFilter.contains(flag)) productFlagsFilter - flag else productFlagsFilter + flag
                        },
                        sort = productSort,
                        onSortChange = { productSort = it },
                        isSortAscending = isProductSortAscending,
                        onSortDirectionChange = { isProductSortAscending = it },
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    PhotoCarousel(
                                        photos = product.photos,
                                        title = "Фото продукта ${product.name}",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("Категория: ${product.category.label}")
                                    Text("Готовка: ${product.cookingRequirement.label}")
                                    Text("КБЖУ/100 г: ${pretty(product.nutritionPer100g)}")
                                    Text("Флаги: ${flagsText(product.flags)}")
                                    Text("Фото: ${if (product.photos.isEmpty()) "нет" else "${product.photos.size} шт."}")
                                    Text("Создан: ${humanReadableDateTime(product.createdAt)}")
                                    Text("Изменён: ${humanReadableDateTime(product.updatedAt)}")
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(onClick = {
                                            detailScreen = DetailScreen.ProductDetails(product)
                                        }) { Text("Просмотр") }
                                        Button(onClick = {
                                            productForm = ProductFormState(
                                                id = product.id,
                                                name = product.name,
                                                photos = product.photos,
                                                calories = product.nutritionPer100g.calories.toString(),
                                                proteins = product.nutritionPer100g.proteins.toString(),
                                                fats = product.nutritionPer100g.fats.toString(),
                                                carbs = product.nutritionPer100g.carbs.toString(),
                                                composition = product.composition.orEmpty(),
                                                category = product.category,
                                                cookingRequirement = product.cookingRequirement,
                                                flags = product.flags,
                                            )
                                            productError = null
                                            isProductEditorVisible = true
                                        }) { Text("Редактировать") }
                                        Button(onClick = {
                                            scope.launch {
                                                runCatching {
                                                    api.deleteProduct(product.id)
                                                    products.remove(product)
                                                }.onFailure { error ->
                                                    val message =
                                                        if (error is HttpException && error.code() == 409) {
                                                            val blocked =
                                                                error.response()?.errorBody()
                                                                    ?.string().orEmpty()
                                                            val parsed = runCatching {
                                                                Gson().fromJson(
                                                                    blocked,
                                                                    DeletionBlockedResponse::class.java
                                                                )
                                                            }.getOrNull()
                                                            val dishNames =
                                                                parsed?.dishNames.orEmpty()
                                                            if (dishNames.isNotEmpty()) {
                                                                "Удаление недоступно: продукт используется в блюдах: ${dishNames.joinToString()}"
                                                            } else {
                                                                "Удаление недоступно: продукт используется в блюдах"
                                                            }
                                                        } else {
                                                            "Ошибка удаления продукта: ${error.message}"
                                                        }
                                                    snackBarHostState.showSnackbar(message)
                                                }
                                            }
                                        }) { Text("Удалить") }
                                    }
                                }
                            }
                        }
                        if (filteredProducts.isEmpty()) {
                            item { Text("Продукты не найдены", modifier = Modifier.padding(8.dp)) }
                        }
                    }
                }
            } else {
                if (!isDishEditorVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            dishForm = DishFormState()
                            dishError = null
                            isDishEditorVisible = true
                        }) { Text("Создать блюдо") }
                    }
                }
                if (isDishEditorVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        DishEditor(
                            api = api,
                            form = dishForm,
                            products = products,
                            error = dishError,
                            onChange = { dishForm = it },
                            onUploadFailure = { message ->
                                scope.launch { snackBarHostState.showSnackbar(message) }
                            },
                            onSave = {
                                runCatching {
                                    require(dishForm.name.trim().length >= 2) { "Название блюда: минимум 2 символа" }
                                    require(dishForm.photos.size <= 5) { "Можно указать не более 5 фото" }
                                    val ingredients =
                                        dishForm.ingredientGrams.mapNotNull { (productId, gramsText) ->
                                            val grams =
                                                gramsText.toDoubleOrNull() ?: return@mapNotNull null
                                            if (grams <= 0) return@mapNotNull null
                                            DishIngredient(productId, grams)
                                        }
                                    require(ingredients.isNotEmpty()) { "Нужно добавить минимум 1 продукт" }
                                    val portion = parseRequiredDouble(
                                        dishForm.portionSize,
                                        "Размер порции"
                                    ).also { require(it > 0) { "Размер порции должен быть больше 0" } }
                                    val (nameForSaving, categoryFromSelectionOrMacro) =
                                        resolveDishNameAndCategoryForSave(
                                            name = dishForm.name,
                                            manuallySelectedCategory = dishForm.category
                                        )
                                    val category = requireNotNull(categoryFromSelectionOrMacro) {
                                        "Укажите категорию или добавьте макрос в названии"
                                    }
                                    val nutrition = Nutrition(
                                        calories = parseRequiredDouble(
                                            dishForm.calories,
                                            "Калорийность"
                                        )
                                            .also { require(it >= 0) { "Калорийность должна быть >= 0" } },
                                        proteins = parseRequiredDouble(dishForm.proteins, "Белки")
                                            .also { require(it >= 0) { "Белки должны быть >= 0" } },
                                        fats = parseRequiredDouble(dishForm.fats, "Жиры")
                                            .also { require(it >= 0) { "Жиры должны быть >= 0" } },
                                        carbs = parseRequiredDouble(dishForm.carbs, "Углеводы")
                                            .also { require(it >= 0) { "Углеводы должны быть >= 0" } },
                                    )
                                    require(nutrition.proteins + nutrition.fats + nutrition.carbs <= 100.0) {
                                        "Сумма БЖУ на порцию не может превышать 100"
                                    }
                                    val allowedFlags = allowedDishFlags(ingredients, productById)
                                    val request = DishUpsertRequest(
                                        name = nameForSaving,
                                        photos = dishForm.photos,
                                        nutritionPerPortion = nutrition,
                                        ingredients = ingredients,
                                        portionSizeGrams = portion,
                                        category = category,
                                        flags = dishForm.flags.intersect(allowedFlags),
                                    )
                                    scope.launch {
                                        runCatching {
                                            val saved = if (dishForm.id == null) {
                                                api.createDish(request)
                                            } else {
                                                api.updateDish(dishForm.id!!, request)
                                            }.withNormalizedPhotoUrls()
                                            val index = dishes.indexOfFirst { it.id == saved.id }
                                            if (index >= 0) dishes[index] =
                                                saved else dishes += saved
                                            dishForm = DishFormState()
                                            dishError = null
                                            isDishEditorVisible = false
                                        }.onFailure {
                                            dishError = it.message ?: "Ошибка сохранения"
                                        }
                                    }
                                }.onFailure { dishError = it.message ?: "Ошибка сохранения" }
                            },
                            onAutoFillNutrition = {
                                val ingredients =
                                    dishForm.ingredientGrams.mapNotNull { (id, gramsText) ->
                                        val grams =
                                            gramsText.toDoubleOrNull() ?: return@mapNotNull null
                                        if (grams <= 0) return@mapNotNull null
                                        DishIngredient(id, grams)
                                    }
                                scope.launch {
                                    runCatching {
                                        val calculation = api.calculateDish(ingredients)
                                        val nutrition = calculation.nutrition
                                        dishForm = dishForm.copy(
                                            calories = nutrition.calories.toOneDecimal(),
                                            proteins = nutrition.proteins.toOneDecimal(),
                                            fats = nutrition.fats.toOneDecimal(),
                                            carbs = nutrition.carbs.toOneDecimal(),
                                            flags = dishForm.flags.intersect(calculation.availableFlags),
                                            isNutritionManuallyEdited = false,
                                        )
                                    }.onFailure {
                                        dishError = "Не удалось рассчитать КБЖУ: ${it.message}"
                                    }
                                }
                            },
                            onCancel = {
                                dishForm = DishFormState()
                                dishError = null
                                isDishEditorVisible = false
                            }
                        )
                    }
                }

                if (!isDishEditorVisible) {
                    DishFilterBlock(
                        search = dishSearch,
                        onSearchChange = { dishSearch = it },
                        categoryFilter = dishCategoryFilter,
                        onCategoryFilterToggle = { category ->
                            dishCategoryFilter =
                                if (dishCategoryFilter.contains(category)) dishCategoryFilter - category
                                else dishCategoryFilter + category
                        },
                        onCategoryFilterReset = { dishCategoryFilter = emptySet() },
                        flagsFilter = dishFlagsFilter,
                        onFlagToggle = { flag ->
                            dishFlagsFilter =
                                if (dishFlagsFilter.contains(flag)) dishFlagsFilter - flag else dishFlagsFilter + flag
                        }
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredDishes, key = { it.id }) { dish ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(dish.name, fontWeight = FontWeight.Bold)
                                    PhotoCarousel(
                                        photos = dish.photos,
                                        title = "Фото блюда ${dish.name}",
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Text("Категория: ${dish.category.label}")
                                    Text("Размер порции: ${dish.portionSizeGrams} г")
                                    Text("КБЖУ/порция: ${pretty(dish.nutritionPerPortion)}")
                                    Text("Флаги: ${flagsText(dish.flags)}")
                                    Text("Фото: ${if (dish.photos.isEmpty()) "нет" else "${dish.photos.size} шт."}")
                                    Text("Создан: ${humanReadableDateTime(dish.createdAt)}")
                                    Text("Изменён: ${humanReadableDateTime(dish.updatedAt)}")
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(onClick = {
                                            detailScreen = DetailScreen.DishDetails(dish)
                                        }) { Text("Просмотр") }
                                        Button(onClick = {
                                            dishForm = DishFormState(
                                                id = dish.id,
                                                name = dish.name,
                                                photos = dish.photos,
                                                calories = dish.nutritionPerPortion.calories.toString(),
                                                proteins = dish.nutritionPerPortion.proteins.toString(),
                                                fats = dish.nutritionPerPortion.fats.toString(),
                                                carbs = dish.nutritionPerPortion.carbs.toString(),
                                                portionSize = dish.portionSizeGrams.toString(),
                                                category = dish.category,
                                                flags = dish.flags,
                                                ingredientGrams = dish.ingredients.associate { it.productId to it.grams.toString() },
                                                isNutritionManuallyEdited = true,
                                            )
                                            dishError = null
                                            isDishEditorVisible = true
                                        }) { Text("Редактировать") }
                                        Button(onClick = {
                                            scope.launch {
                                                runCatching {
                                                    api.deleteDish(dish.id)
                                                    dishes.remove(dish)
                                                }.onFailure {
                                                    snackBarHostState.showSnackbar("Ошибка удаления блюда: ${it.message}")
                                                }
                                            }
                                        }) { Text("Удалить") }
                                    }
                                }
                            }
                        }
                        if (filteredDishes.isEmpty()) {
                            item { Text("Блюда не найдены", modifier = Modifier.padding(8.dp)) }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(dishForm.ingredientGrams, products.size, tab, isDishEditorVisible) {
        if (tab == 1 && isDishEditorVisible) {
            val ingredients = dishForm.ingredientGrams.mapNotNull { (id, gramsText) ->
                val grams = gramsText.toDoubleOrNull() ?: return@mapNotNull null
                if (grams <= 0) return@mapNotNull null
                DishIngredient(id, grams)
            }
            val allowed = allowedDishFlags(ingredients, productById)
            val nutrition = calculateNutrition(ingredients, productById)
            dishForm = dishForm.copy(
                flags = dishForm.flags.intersect(allowed),
                calories = if (dishForm.isNutritionManuallyEdited) dishForm.calories else nutrition.calories.toOneDecimal(),
                proteins = if (dishForm.isNutritionManuallyEdited) dishForm.proteins else nutrition.proteins.toOneDecimal(),
                fats = if (dishForm.isNutritionManuallyEdited) dishForm.fats else nutrition.fats.toOneDecimal(),
                carbs = if (dishForm.isNutritionManuallyEdited) dishForm.carbs else nutrition.carbs.toOneDecimal(),
            )
        }
    }
}

@Composable
private fun ProductEditor(
    api: RecipeBookApi,
    form: ProductFormState,
    error: String?,
    onChange: (ProductFormState) -> Unit,
    onUploadFailure: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null || form.photos.size >= 5) return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    uploadImage(
                        context.contentResolver,
                        api,
                        uri.toString(),
                        uri.lastPathSegment
                    )
                }
                    .onSuccess { uploadedUrl -> onChange(form.copy(photos = form.photos + uploadedUrl)) }
                    .onFailure { onUploadFailure("Не удалось загрузить фото: ${it.message}") }
            }
        }

    Column(
        Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (form.id == null) "Создание продукта" else "Редактирование продукта",
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            form.name,
            { onChange(form.copy(name = it)) },
            label = { Text("Название*") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { launcher.launch("image/*") }) {
                Text(
                    "Добавить фото"
                )
            }
            Text("Выбрано: ${form.photos.size}/5")
        }
        if (form.photos.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                form.photos.forEachIndexed { index, _ ->
                    FilterChip(
                        selected = false,
                        onClick = { onChange(form.copy(photos = form.photos.filterIndexed { i, _ -> i != index })) },
                        label = { Text("Фото ${index + 1} ✕") }
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NutritionField("Ккал", form.calories) { onChange(form.copy(calories = it)) }
            NutritionField("Белки", form.proteins) { onChange(form.copy(proteins = it)) }
            NutritionField("Жиры", form.fats) { onChange(form.copy(fats = it)) }
            NutritionField("Углев.", form.carbs) { onChange(form.copy(carbs = it)) }
        }
        OutlinedTextField(
            form.composition,
            { onChange(form.copy(composition = it)) },
            label = { Text("Состав") },
            modifier = Modifier.fillMaxWidth()
        )

        EnumChoice("Категория", ProductCategory.entries, form.category, { it.label }) {
            onChange(
                form.copy(category = it)
            )
        }
        EnumChoice(
            "Необходимость готовки",
            CookingRequirement.entries,
            form.cookingRequirement,
            { it.label }) { onChange(form.copy(cookingRequirement = it)) }
        FlagSelector(form.flags) { flag ->
            onChange(form.copy(flags = if (form.flags.contains(flag)) form.flags - flag else form.flags + flag))
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSave) { Text(if (form.id == null) "Создать" else "Сохранить") }
            Button(onClick = onCancel) { Text("Отмена") }
        }
    }
}

@Composable
private fun DishEditor(
    api: RecipeBookApi,
    form: DishFormState,
    products: List<Product>,
    error: String?,
    onChange: (DishFormState) -> Unit,
    onUploadFailure: (String) -> Unit,
    onSave: () -> Unit,
    onAutoFillNutrition: () -> Unit,
    onCancel: () -> Unit,
) {
    val productById = products.associateBy { it.id }
    val ingredients = form.ingredientGrams.mapNotNull { (id, gramsText) ->
        val grams = gramsText.toDoubleOrNull() ?: return@mapNotNull null
        if (grams <= 0) return@mapNotNull null
        DishIngredient(id, grams)
    }
    val allowedFlags = allowedDishFlags(ingredients, productById)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null || form.photos.size >= 5) return@rememberLauncherForActivityResult
            scope.launch {
                runCatching {
                    uploadImage(
                        context.contentResolver,
                        api,
                        uri.toString(),
                        uri.lastPathSegment
                    )
                }
                    .onSuccess { uploadedUrl -> onChange(form.copy(photos = form.photos + uploadedUrl)) }
                    .onFailure { onUploadFailure("Не удалось загрузить фото: ${it.message}") }
            }
        }

    Column(
        Modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            if (form.id == null) "Создание блюда" else "Редактирование блюда",
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            form.name,
            { onChange(form.copy(name = it)) },
            label = { Text("Название* (макросы: !десерт, !первое...)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            form.portionSize,
            { onChange(form.copy(portionSize = it)) },
            label = { Text("Размер порции, г*") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { launcher.launch("image/*") }) {
                Text(
                    "Добавить фото"
                )
            }
            Text("Выбрано: ${form.photos.size}/5")
        }
        if (form.photos.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                form.photos.forEachIndexed { index, _ ->
                    FilterChip(
                        selected = false,
                        onClick = { onChange(form.copy(photos = form.photos.filterIndexed { i, _ -> i != index })) },
                        label = { Text("Фото ${index + 1} ✕") }
                    )
                }
            }
        }

        Text("Категория (если не указать — берём из макроса в названии)")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = form.category == null,
                onClick = { onChange(form.copy(category = null)) },
                label = { Text("Авто") })
            DishCategory.entries.forEach { category ->
                FilterChip(
                    selected = form.category == category,
                    onClick = { onChange(form.copy(category = category)) },
                    label = { Text(category.label) })
            }
        }

        Text("Состав блюда (минимум 1 продукт)", fontWeight = FontWeight.Medium)
        products.forEach { product ->
            val selected = form.ingredientGrams.containsKey(product.id)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(checked = selected, onCheckedChange = { check ->
                    onChange(
                        if (check) form.copy(ingredientGrams = form.ingredientGrams + (product.id to "100"))
                        else form.copy(ingredientGrams = form.ingredientGrams - product.id)
                    )
                })
                Text(product.name, modifier = Modifier.weight(1f))
                if (selected) {
                    OutlinedTextField(
                        form.ingredientGrams[product.id].orEmpty(),
                        onValueChange = { grams -> onChange(form.copy(ingredientGrams = form.ingredientGrams + (product.id to grams))) },
                        label = { Text("г") },
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NutritionField("Ккал", form.calories) {
                onChange(
                    form.copy(
                        calories = it,
                        isNutritionManuallyEdited = true
                    )
                )
            }
            NutritionField("Белки", form.proteins) {
                onChange(
                    form.copy(
                        proteins = it,
                        isNutritionManuallyEdited = true
                    )
                )
            }
            NutritionField("Жиры", form.fats) {
                onChange(
                    form.copy(
                        fats = it,
                        isNutritionManuallyEdited = true
                    )
                )
            }
            NutritionField("Углев.", form.carbs) {
                onChange(
                    form.copy(
                        carbs = it,
                        isNutritionManuallyEdited = true
                    )
                )
            }
        }
        Button(onClick = onAutoFillNutrition) { Text("Автоматически рассчитать КБЖУ") }

        Text("Флаги блюда")
        ExtraFlag.entries.forEach { flag ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = form.flags.contains(flag),
                    enabled = allowedFlags.contains(flag),
                    onCheckedChange = { checked ->
                        onChange(
                            form.copy(
                                flags = if (checked) form.flags + flag else form.flags - flag
                            )
                        )
                    }
                )
                Text("${flag.label}${if (!allowedFlags.contains(flag)) " (недоступно по составу)" else ""}")
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onSave) { Text(if (form.id == null) "Создать" else "Сохранить") }
            Button(onClick = onCancel) { Text("Отмена") }
        }
        HorizontalDivider()
    }
}

@Composable
private fun ProductFilterBlock(
    search: String,
    onSearchChange: (String) -> Unit,
    categoryFilter: Set<ProductCategory>,
    onCategoryFilterToggle: (ProductCategory) -> Unit,
    onCategoryFilterReset: () -> Unit,
    cookingFilter: Set<CookingRequirement>,
    onCookingFilterToggle: (CookingRequirement) -> Unit,
    onCookingFilterReset: () -> Unit,
    flagsFilter: Set<ExtraFlag>,
    onFlagToggle: (ExtraFlag) -> Unit,
    sort: ProductSort,
    onSortChange: (ProductSort) -> Unit,
    isSortAscending: Boolean,
    onSortDirectionChange: (Boolean) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            search,
            onSearchChange,
            label = { Text("Поиск продукта") },
            modifier = Modifier.fillMaxWidth()
        )
        FilledTonalButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isExpanded) "Скрыть фильтры и сортировку" else "Открыть фильтры и сортировку")
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Категория")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = categoryFilter.isEmpty(),
                        onClick = onCategoryFilterReset,
                        label = { Text("Все") })
                    ProductCategory.entries.forEach {
                        FilterChip(
                            selected = categoryFilter.contains(it),
                            onClick = { onCategoryFilterToggle(it) },
                            label = { Text(it.label) })
                    }
                }
                Text("Готовка")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = cookingFilter.isEmpty(),
                        onClick = onCookingFilterReset,
                        label = { Text("Все") })
                    CookingRequirement.entries.forEach {
                        FilterChip(
                            selected = cookingFilter.contains(it),
                            onClick = { onCookingFilterToggle(it) },
                            label = { Text(it.label) })
                    }
                }
                Text("Флаги")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExtraFlag.entries.forEach { flag ->
                        FilterChip(
                            selected = flagsFilter.contains(flag),
                            onClick = { onFlagToggle(flag) },
                            label = { Text(flag.label) })
                    }
                }
                Text("Сортировка")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProductSort.entries.forEach { option ->
                        ElevatedFilterChip(
                            selected = sort == option,
                            onClick = { onSortChange(option) },
                            label = { Text(option.label) }
                        )
                    }
                    ElevatedFilterChip(
                        selected = true,
                        onClick = { onSortDirectionChange(!isSortAscending) },
                        label = { Text(if (isSortAscending) "↑ По возрастанию" else "↓ По убыванию") }
                    )
                }
                Divider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun DishFilterBlock(
    search: String,
    onSearchChange: (String) -> Unit,
    categoryFilter: Set<DishCategory>,
    onCategoryFilterToggle: (DishCategory) -> Unit,
    onCategoryFilterReset: () -> Unit,
    flagsFilter: Set<ExtraFlag>,
    onFlagToggle: (ExtraFlag) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            search,
            onSearchChange,
            label = { Text("Поиск блюда") },
            modifier = Modifier.fillMaxWidth()
        )
        FilledTonalButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isExpanded) "Скрыть фильтры" else "Открыть фильтры")
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Категория")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = categoryFilter.isEmpty(),
                        onClick = onCategoryFilterReset,
                        label = { Text("Все") })
                    DishCategory.entries.forEach {
                        FilterChip(
                            selected = categoryFilter.contains(it),
                            onClick = { onCategoryFilterToggle(it) },
                            label = { Text(it.label) })
                    }
                }
                Text("Флаги")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExtraFlag.entries.forEach { flag ->
                        FilterChip(
                            selected = flagsFilter.contains(flag),
                            onClick = { onFlagToggle(flag) },
                            label = { Text(flag.label) })
                    }
                }
                Divider(Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun NutritionField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.width(120.dp)
    )
}

@Composable
private fun <T> EnumChoice(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Text(title)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(label(option)) })
        }
    }
}

@Composable
private fun FlagSelector(flags: Set<ExtraFlag>, onToggle: (ExtraFlag) -> Unit) {
    Text("Дополнительные флаги")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ExtraFlag.entries.forEach { flag ->
            FilterChip(
                selected = flags.contains(flag),
                onClick = { onToggle(flag) },
                label = { Text(flag.label) })
        }
    }
}

@Composable
private fun PhotoCarousel(
    photos: List<String>,
    title: String,
    modifier: Modifier = Modifier,
    imageHeight: Int? = 160,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (photos.isEmpty()) return
    var index by remember(photos) { mutableIntStateOf(0) }
    val currentPhoto = photos[index]
    val decodedBitmap = remember(currentPhoto) { decodeDataImageToBitmapOrNull(currentPhoto) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val imageModifier = Modifier
            .fillMaxWidth()
            .then(if (imageHeight != null) Modifier.height(imageHeight.dp) else Modifier)
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = "$title ${index + 1}",
                modifier = imageModifier,
                contentScale = contentScale,
            )
        } else {
            AsyncImage(
                model = currentPhoto,
                contentDescription = "$title ${index + 1}",
                modifier = imageModifier,
                contentScale = contentScale,
            )
        }
        if (photos.size > 1) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { index = if (index == 0) photos.lastIndex else index - 1 }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущее фото")
                }
                Text("${index + 1} / ${photos.size}")
                IconButton(onClick = { index = if (index == photos.lastIndex) 0 else index + 1 }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующее фото")
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsScreen(product: Product, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Просмотр продукта", fontWeight = FontWeight.SemiBold)
        }
        Text("Название: ${product.name}")
        PhotoCarousel(
            product.photos,
            "Фото продукта ${product.name}",
            imageHeight = null,
            contentScale = ContentScale.Fit,
        )
        Text("Категория: ${product.category.label}")
        Text("Готовка: ${product.cookingRequirement.label}")
        Text("КБЖУ/100 г: ${pretty(product.nutritionPer100g)}")
        Text("Флаги: ${flagsText(product.flags)}")
        Text("Состав: ${product.composition ?: "—"}")
        Text("Создан: ${humanReadableDateTime(product.createdAt)}")
        Text("Изменён: ${humanReadableDateTime(product.updatedAt)}")
    }
}

@Composable
private fun DishDetailsScreen(
    dish: Dish,
    productsById: Map<String, Product>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Просмотр блюда", fontWeight = FontWeight.SemiBold)
        }
        Text("Название: ${dish.name}")
        PhotoCarousel(
            dish.photos,
            "Фото блюда ${dish.name}",
            imageHeight = null,
            contentScale = ContentScale.Fit,
        )
        Text("Категория: ${dish.category.label}")
        Text("Размер порции: ${dish.portionSizeGrams} г")
        Text("КБЖУ/порция: ${pretty(dish.nutritionPerPortion)}")
        Text("Флаги: ${flagsText(dish.flags)}")
        Text("Состав:")
        dish.ingredients.forEach { ingredient ->
            Text("- ${productsById[ingredient.productId]?.name ?: "?"}: ${ingredient.grams.toOneDecimal()} г")
        }
        Text("Создан: ${humanReadableDateTime(dish.createdAt)}")
        Text("Изменён: ${humanReadableDateTime(dish.updatedAt)}")
    }
}

private fun parseRequiredDouble(value: String, fieldName: String): Double {
    require(value.isNotBlank()) { "Поле \"$fieldName\" не может быть пустым" }
    return requireNotNull(value.toDoubleOrNull()) { "Поле \"$fieldName\" должно быть числом" }
}

private fun pretty(nutrition: Nutrition): String =
    "${nutrition.calories.toOneDecimal()} / ${nutrition.proteins.toOneDecimal()} / ${nutrition.fats.toOneDecimal()} / ${nutrition.carbs.toOneDecimal()}"

private fun flagsText(flags: Set<ExtraFlag>): String =
    flags.takeIf { it.isNotEmpty() }?.joinToString { it.label } ?: "нет"

private fun humanReadableDateTime(rawDateTime: String?): String {
    if (rawDateTime.isNullOrBlank()) return "—"
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru", "RU"))

    val zonedDateTime = parseToInstantOrNull(rawDateTime)
        ?.atZone(TOMSK_ZONE_ID)
        ?.toLocalDateTime()
        ?: parseNaiveUtcToTomskLocalDateTimeOrNull(rawDateTime)
        ?: parseToLocalDateTimeOrNull(rawDateTime)
        ?: return rawDateTime.substringBefore("T").takeIf { it.isNotBlank() } ?: rawDateTime

    return zonedDateTime.format(formatter)
}

private fun parseToInstantOrNull(value: String): Instant? {
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: value.toLongOrNull()?.let { epoch ->
            runCatching { Instant.ofEpochMilli(epoch) }.getOrNull()
                ?: runCatching { Instant.ofEpochSecond(epoch) }.getOrNull()
        }
}

private fun parseToLocalDateTimeOrNull(value: String): LocalDateTime? {
    val localPatterns = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
    )
    for (pattern in localPatterns) {
        try {
            return LocalDateTime.parse(value, pattern)
        } catch (_: DateTimeParseException) {
            // try next pattern
        }
    }
    return null
}

private fun parseNaiveUtcToTomskLocalDateTimeOrNull(value: String): LocalDateTime? {
    val hasExplicitTimezone =
        value.endsWith("Z", ignoreCase = true) ||
                Regex("[+-]\\d{2}:\\d{2}$").containsMatchIn(value)
    if (hasExplicitTimezone) return null

    val localDateTime = parseToLocalDateTimeOrNull(value) ?: return null
    return localDateTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(TOMSK_ZONE_ID).toLocalDateTime()
}

private suspend fun uploadImage(
    contentResolver: android.content.ContentResolver,
    api: RecipeBookApi,
    uriString: String,
    originalName: String?,
): String {
    val uri = android.net.Uri.parse(uriString)
    val bytes =
        requireNotNull(contentResolver.openInputStream(uri)) { "Не удалось открыть изображение" }.use { it.readBytes() }
    val fileName = originalName?.takeIf { it.isNotBlank() } ?: "photo.jpg"
    return runCatching {
        val requestBody = bytes.toRequestBody("image/*".toMediaType())
        val part = MultipartBody.Part.createFormData("photo", fileName, requestBody)
        val uploadedUrl = api.uploadPhoto(part).url
        if (uploadedUrl.startsWith("http://") || uploadedUrl.startsWith("https://")) {
            uploadedUrl
        } else {
            "${RecipeBookApiFactory.BASE_URL.trimEnd('/')}/${uploadedUrl.trimStart('/')}"
        }
    }.getOrElse {
        encodePhotoAsDataUrl(contentResolver, uri)
    }
}

private fun encodePhotoAsDataUrl(
    contentResolver: android.content.ContentResolver,
    uri: android.net.Uri,
): String {
    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    requireNotNull(contentResolver.openInputStream(uri)) { "Не удалось открыть изображение" }.use { input ->
        android.graphics.BitmapFactory.decodeStream(input, null, options)
    }

    val maxDimension = 1280
    var inSampleSize = 1
    while (options.outWidth / inSampleSize > maxDimension || options.outHeight / inSampleSize > maxDimension) {
        inSampleSize *= 2
    }

    val decodeOptions =
        android.graphics.BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
    val bitmap =
        requireNotNull(contentResolver.openInputStream(uri)) { "Не удалось открыть изображение" }.use { input ->
            android.graphics.BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: error("Не удалось декодировать изображение")

    val output = ByteArrayOutputStream()
    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, output)
    bitmap.recycle()

    val base64 = Base64.getEncoder().encodeToString(output.toByteArray())
    return "data:image/jpeg;base64,$base64"
}

private fun Double.toOneDecimal(): String = "%.1f".format(this)

private fun Product.withNormalizedPhotoUrls(): Product = copy(
    photos = photos.map { normalizePhotoUrl(it) }
)

private fun Dish.withNormalizedPhotoUrls(): Dish = copy(
    photos = photos.map { normalizePhotoUrl(it) }
)

private fun normalizePhotoUrl(url: String): String {
    if (
        url.startsWith("http://") ||
        url.startsWith("https://") ||
        url.startsWith("data:image", ignoreCase = true) ||
        url.startsWith("content://") ||
        url.startsWith("file://")
    ) {
        return url
    }

    return "${RecipeBookApiFactory.BASE_URL.trimEnd('/')}/${url.trimStart('/')}"
}

private fun decodeDataImageToBitmapOrNull(photo: String): android.graphics.Bitmap? {
    if (!photo.startsWith("data:image", ignoreCase = true)) return null
    val delimiter = ";base64,"
    val base64Start = photo.indexOf(delimiter, ignoreCase = true)
    if (base64Start == -1) return null

    val payload = photo.substring(base64Start + delimiter.length)
    val sanitizedPayload = payload.filterNot { it == '\n' || it == '\r' || it == ' ' || it == '\t' }
    val bytes =
        runCatching { Base64.getDecoder().decode(sanitizedPayload) }.getOrNull() ?: return null
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    RecipeBookTheme { RecipeBookApp() }
}