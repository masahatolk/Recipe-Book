package com.hits.recipebook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hits.recipebook.ui.theme.RecipeBookTheme
import kotlinx.coroutines.launch
import java.time.Instant

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
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeBookApp() {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val products = remember {
        mutableStateListOf(
            Product(
                name = "Тофу",
                category = ProductCategory.MEAT,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
                nutritionPer100g = Nutrition(120.0, 12.0, 7.0, 1.5),
                composition = "Соевые бобы, вода",
            ),
            Product(
                name = "Томат",
                category = ProductCategory.VEGETABLES,
                cookingRequirement = CookingRequirement.READY_TO_EAT,
                flags = setOf(ExtraFlag.VEGAN, ExtraFlag.GLUTEN_FREE, ExtraFlag.SUGAR_FREE),
                nutritionPer100g = Nutrition(18.0, 0.9, 0.2, 3.9),
            )
        )
    }

    val dishes = remember { mutableStateListOf<Dish>() }

    var tab by remember { mutableIntStateOf(0) }
    var productSearch by remember { mutableStateOf("") }
    var productCategoryFilter by remember { mutableStateOf<ProductCategory?>(null) }
    var cookingFilter by remember { mutableStateOf<CookingRequirement?>(null) }
    var productFlagsFilter by remember { mutableStateOf(setOf<ExtraFlag>()) }
    var productSort by remember { mutableStateOf(ProductSort.NAME) }
    var dishSearch by remember { mutableStateOf("") }
    var dishCategoryFilter by remember { mutableStateOf<DishCategory?>(null) }
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

    val filteredProducts = products
        .filter { productSearch.isBlank() || it.name.contains(productSearch, ignoreCase = true) }
        .filter { productCategoryFilter == null || it.category == productCategoryFilter }
        .filter { cookingFilter == null || it.cookingRequirement == cookingFilter }
        .filter { it.flags.containsAll(productFlagsFilter) }
        .sortedWith(
            when (productSort) {
                ProductSort.NAME -> compareBy { it.name.lowercase() }
                ProductSort.CALORIES -> compareBy { it.nutritionPer100g.calories }
                ProductSort.PROTEINS -> compareBy { it.nutritionPer100g.proteins }
                ProductSort.FATS -> compareBy { it.nutritionPer100g.fats }
                ProductSort.CARBS -> compareBy { it.nutritionPer100g.carbs }
            }
        )

    val filteredDishes = dishes
        .filter { dishSearch.isBlank() || it.name.contains(dishSearch, ignoreCase = true) }
        .filter { dishCategoryFilter == null || it.category == dishCategoryFilter }
        .filter { it.flags.containsAll(dishFlagsFilter) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {
            TabRow(tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Продукты") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Блюда") })
            }
            if (tab == 0) {
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
                if (isProductEditorVisible) {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)) {
                        ProductEditor(
                            form = productForm,
                            error = productError,
                            onChange = { productForm = it },
                            onSave = {
                                runCatching {
                                    require(productForm.name.trim().length >= 2) { "Название продукта: минимум 2 символа" }
                                    require(productForm.photos.size <= 1) { "Можно указать только 1 фото" }
                                    val nutrition = Nutrition(
                                        calories = productForm.calories.toDouble()
                                            .also { require(it >= 0) },
                                        proteins = productForm.proteins.toDouble()
                                            .also { require(it >= 0) },
                                        fats = productForm.fats.toDouble()
                                            .also { require(it >= 0) },
                                        carbs = productForm.carbs.toDouble()
                                            .also { require(it >= 0) },
                                    )
                                    val entity = Product(
                                        id = productForm.id ?: java.util.UUID.randomUUID()
                                            .toString(),
                                        name = productForm.name.trim(),
                                        photos = productForm.photos,
                                        nutritionPer100g = nutrition,
                                        composition = productForm.composition.ifBlank { null },
                                        category = productForm.category,
                                        cookingRequirement = productForm.cookingRequirement,
                                        flags = productForm.flags,
                                        createdAt = products.firstOrNull { it.id == productForm.id }?.createdAt
                                            ?: Instant.now().toString(),
                                        updatedAt = if (productForm.id == null) null else Instant.now()
                                            .toString(),
                                    )
                                    val currentIndex = products.indexOfFirst { it.id == entity.id }
                                    if (currentIndex >= 0) products[currentIndex] =
                                        entity else products += entity
                                    productForm = ProductFormState()
                                    productError = null
                                    isProductEditorVisible = false
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

                ProductFilterBlock(
                    search = productSearch,
                    onSearchChange = { productSearch = it },
                    categoryFilter = productCategoryFilter,
                    onCategoryFilterChange = { productCategoryFilter = it },
                    cookingFilter = cookingFilter,
                    onCookingFilterChange = { cookingFilter = it },
                    flagsFilter = productFlagsFilter,
                    onFlagToggle = { flag ->
                        productFlagsFilter =
                            if (productFlagsFilter.contains(flag)) productFlagsFilter - flag else productFlagsFilter + flag
                    },
                    sort = productSort,
                    onSortChange = { productSort = it }
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
                                product.photos.firstOrNull()?.let { photo ->
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = "Фото продукта ${product.name}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                Text("Категория: ${product.category.label}")
                                Text("Готовка: ${product.cookingRequirement.label}")
                                Text("КБЖУ/100 г: ${pretty(product.nutritionPer100g)}")
                                Text("Флаги: ${flagsText(product.flags)}")
                                Text("Фото: ${if (product.photos.isEmpty()) "нет" else "1 шт."}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        selectedProduct = product
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
                                        val usedBy =
                                            dishes.filter { d -> d.ingredients.any { it.productId == product.id } }
                                        if (usedBy.isNotEmpty()) {
                                            scope.launch {
                                                snackBarHostState.showSnackbar(
                                                    "Удаление недоступно. Используется в блюдах: ${usedBy.joinToString { it.name }}"
                                                )
                                            }
                                        } else {
                                            products.remove(product)
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
            } else {
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
                if (isDishEditorVisible) {
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)) {
                        DishEditor(
                            form = dishForm,
                            products = products,
                            error = dishError,
                            onChange = { dishForm = it },
                            onSave = {
                                runCatching {
                                    require(dishForm.name.trim().length >= 2) { "Название блюда: минимум 2 символа" }
                                    require(dishForm.photos.size <= 1) { "Можно указать только 1 фото" }
                                    val ingredients =
                                        dishForm.ingredientGrams.mapNotNull { (productId, gramsText) ->
                                            val grams =
                                                gramsText.toDoubleOrNull() ?: return@mapNotNull null
                                            if (grams <= 0) return@mapNotNull null
                                            DishIngredient(productId, grams)
                                        }
                                    require(ingredients.isNotEmpty()) { "Нужно добавить минимум 1 продукт" }
                                    val portion =
                                        dishForm.portionSize.toDouble().also { require(it > 0) }
                                    val (cleanName, macroCategory) = resolveDishNameAndMacroCategory(
                                        dishForm.name
                                    )
                                    val category = dishForm.category ?: macroCategory
                                    requireNotNull(category) { "Укажите категорию или добавьте макрос в названии" }
                                    val nutrition = Nutrition(
                                        calories = dishForm.calories.toDouble()
                                            .also { require(it >= 0) },
                                        proteins = dishForm.proteins.toDouble()
                                            .also { require(it >= 0) },
                                        fats = dishForm.fats.toDouble().also { require(it >= 0) },
                                        carbs = dishForm.carbs.toDouble().also { require(it >= 0) },
                                    )
                                    val allowedFlags = allowedDishFlags(ingredients, productById)
                                    val entity = Dish(
                                        id = dishForm.id ?: java.util.UUID.randomUUID().toString(),
                                        name = cleanName,
                                        photos = dishForm.photos,
                                        nutritionPerPortion = nutrition,
                                        ingredients = ingredients,
                                        portionSizeGrams = portion,
                                        category = category,
                                        flags = dishForm.flags.intersect(allowedFlags),
                                        createdAt = dishes.firstOrNull { it.id == dishForm.id }?.createdAt
                                            ?: Instant.now().toString(),
                                        updatedAt = if (dishForm.id == null) null else Instant.now()
                                            .toString(),
                                    )
                                    val index = dishes.indexOfFirst { it.id == entity.id }
                                    if (index >= 0) dishes[index] = entity else dishes += entity
                                    dishForm = DishFormState()
                                    dishError = null
                                    isDishEditorVisible = false
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
                                val nutrition = calculateNutrition(ingredients, productById)
                                val allowedFlags = allowedDishFlags(ingredients, productById)
                                dishForm = dishForm.copy(
                                    calories = nutrition.calories.toOneDecimal(),
                                    proteins = nutrition.proteins.toOneDecimal(),
                                    fats = nutrition.fats.toOneDecimal(),
                                    carbs = nutrition.carbs.toOneDecimal(),
                                    flags = dishForm.flags.intersect(allowedFlags),
                                )
                            },
                            onCancel = {
                                dishForm = DishFormState()
                                dishError = null
                                isDishEditorVisible = false
                            }
                        )
                    }
                }

                DishFilterBlock(
                    search = dishSearch,
                    onSearchChange = { dishSearch = it },
                    categoryFilter = dishCategoryFilter,
                    onCategoryFilterChange = { dishCategoryFilter = it },
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
                                dish.photos.firstOrNull()?.let { photo ->
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = "Фото блюда ${dish.name}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                                Text("Категория: ${dish.category.label}")
                                Text("Размер порции: ${dish.portionSizeGrams} г")
                                Text("КБЖУ/порция: ${pretty(dish.nutritionPerPortion)}")
                                Text("Флаги: ${flagsText(dish.flags)}")
                                Text("Фото: ${if (dish.photos.isEmpty()) "нет" else "1 шт."}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { selectedDish = dish }) { Text("Просмотр") }
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
                                            ingredientGrams = dish.ingredients.associate { it.productId to it.grams.toString() }
                                        )
                                        dishError = null
                                        isDishEditorVisible = true
                                    }) { Text("Редактировать") }
                                    Button(onClick = { dishes.remove(dish) }) { Text("Удалить") }
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

    LaunchedEffect(dishForm.ingredientGrams, products.size, tab, isDishEditorVisible) {
        if (tab == 1 && isDishEditorVisible) {
            val ingredients = dishForm.ingredientGrams.mapNotNull { (id, gramsText) ->
                val grams = gramsText.toDoubleOrNull() ?: return@mapNotNull null
                if (grams <= 0) return@mapNotNull null
                DishIngredient(id, grams)
            }
            val allowed = allowedDishFlags(ingredients, productById)
            val nutrition = calculateNutrition(ingredients, productById)
            dishForm = dishForm.copy(flags = dishForm.flags.intersect(allowed)).copy(
                calories = nutrition.calories.toOneDecimal(),
                proteins = nutrition.proteins.toOneDecimal(),
                fats = nutrition.fats.toOneDecimal(),
                carbs = nutrition.carbs.toOneDecimal(),
            )
        }
    }
    selectedProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            confirmButton = {
                TextButton(onClick = {
                    selectedProduct = null
                }) { Text("Закрыть") }
            },
            title = { Text("Просмотр продукта") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Название: ${product.name}")
                    Text("Категория: ${product.category.label}")
                    Text("Готовка: ${product.cookingRequirement.label}")
                    Text("КБЖУ/100 г: ${pretty(product.nutritionPer100g)}")
                    Text("Флаги: ${flagsText(product.flags)}")
                    Text("Состав: ${product.composition ?: "—"}")
                    Text("Фото: ${product.photos.joinToString().ifBlank { "—" }}")
                    Text("Создан: ${product.createdAt}")
                    Text("Изменён: ${product.updatedAt ?: "—"}")
                }
            }
        )
    }

    selectedDish?.let { dish ->
        AlertDialog(
            onDismissRequest = { selectedDish = null },
            confirmButton = { TextButton(onClick = { selectedDish = null }) { Text("Закрыть") } },
            title = { Text("Просмотр блюда") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Название: ${dish.name}")
                    Text("Категория: ${dish.category.label}")
                    Text("Размер порции: ${dish.portionSizeGrams} г")
                    Text("КБЖУ/порция: ${pretty(dish.nutritionPerPortion)}")
                    Text("Флаги: ${flagsText(dish.flags)}")
                    Text("Состав: ${dish.ingredients.joinToString { ingredient -> "${productById[ingredient.productId]?.name ?: "?"}: ${ingredient.grams} г" }}")
                    Text("Фото: ${dish.photos.joinToString().ifBlank { "—" }}")
                    Text("Создан: ${dish.createdAt}")
                    Text("Изменён: ${dish.updatedAt ?: "—"}")
                }
            }
        )
    }
}

@Composable
private fun ProductEditor(
    form: ProductFormState,
    error: String?,
    onChange: (ProductFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            onChange(form.copy(photos = uri?.let { listOf(it.toString()) } ?: emptyList()))
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
                    "Выбрать фото (1)"
                )
            }
            Text("Выбрано: ${form.photos.size}/1")
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text(if (form.id == null) "Создать" else "Сохранить") }
            Button(onClick = onCancel) { Text("Отмена") }
        }
    }
}

@Composable
private fun DishEditor(
    form: DishFormState,
    products: List<Product>,
    error: String?,
    onChange: (DishFormState) -> Unit,
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

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            onChange(form.copy(photos = uri?.let { listOf(it.toString()) } ?: emptyList()))
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
            label = { Text("Название* (макросы: !десерт, !первое...) ") },
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
                    "Выбрать фото (1)"
                )
            }
            Text("Выбрано: ${form.photos.size}/1")
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
            NutritionField("Ккал", form.calories) { onChange(form.copy(calories = it)) }
            NutritionField("Белки", form.proteins) { onChange(form.copy(proteins = it)) }
            NutritionField("Жиры", form.fats) { onChange(form.copy(fats = it)) }
            NutritionField("Углев.", form.carbs) { onChange(form.copy(carbs = it)) }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    categoryFilter: ProductCategory?,
    onCategoryFilterChange: (ProductCategory?) -> Unit,
    cookingFilter: CookingRequirement?,
    onCookingFilterChange: (CookingRequirement?) -> Unit,
    flagsFilter: Set<ExtraFlag>,
    onFlagToggle: (ExtraFlag) -> Unit,
    sort: ProductSort,
    onSortChange: (ProductSort) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            search,
            onSearchChange,
            label = { Text("Поиск продукта") },
            modifier = Modifier.fillMaxWidth()
        )
        FilledTonalButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isExpanded) "Скрыть фильтры и сортировку" else "Открыть фильтры и сортировку")
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isExpanded) {
            Text("Категория")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { onCategoryFilterChange(null) },
                    label = { Text("Все") })
                ProductCategory.entries.forEach {
                    FilterChip(
                        selected = categoryFilter == it,
                        onClick = { onCategoryFilterChange(it) },
                        label = { Text(it.label) })
                }
            }
            Text("Готовка")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = cookingFilter == null,
                    onClick = { onCookingFilterChange(null) },
                    label = { Text("Все") })
                CookingRequirement.entries.forEach {
                    FilterChip(
                        selected = cookingFilter == it,
                        onClick = { onCookingFilterChange(it) },
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
            }
            Divider(Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun DishFilterBlock(
    search: String,
    onSearchChange: (String) -> Unit,
    categoryFilter: DishCategory?,
    onCategoryFilterChange: (DishCategory?) -> Unit,
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
        FilledTonalButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isExpanded) "Скрыть фильтры" else "Открыть фильтры")
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (isExpanded) {
            Text("Категория")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = categoryFilter == null,
                    onClick = { onCategoryFilterChange(null) },
                    label = { Text("Все") })
                DishCategory.entries.forEach {
                    FilterChip(
                        selected = categoryFilter == it,
                        onClick = { onCategoryFilterChange(it) },
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

private fun pretty(nutrition: Nutrition): String =
    "${nutrition.calories.toOneDecimal()} / ${nutrition.proteins.toOneDecimal()} / ${nutrition.fats.toOneDecimal()} / ${nutrition.carbs.toOneDecimal()}"

private fun flagsText(flags: Set<ExtraFlag>): String =
    flags.takeIf { it.isNotEmpty() }?.joinToString { it.label } ?: "нет"

private fun Double.toOneDecimal(): String = "%.1f".format(this)

@Preview(showBackground = true)
@Composable
private fun AppPreview() {
    RecipeBookTheme { RecipeBookApp() }
}