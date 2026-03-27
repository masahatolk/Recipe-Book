package com.hits.recipebook.network

import com.hits.recipebook.CookingRequirement
import com.hits.recipebook.Dish
import com.hits.recipebook.DishCategory
import com.hits.recipebook.DishIngredient
import com.hits.recipebook.ExtraFlag
import com.hits.recipebook.Nutrition
import com.hits.recipebook.Product
import com.hits.recipebook.ProductCategory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeBookApi {
    @GET("api/products")
    suspend fun getProducts(
        @Query("category") category: ProductCategory? = null,
        @Query("cookingRequirement") cookingRequirement: CookingRequirement? = null,
        @Query("flag") flags: MutableList<ExtraFlag>? = null,
        @Query("query") query: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("direction") direction: String? = null,
    ): List<Product>

    @POST("api/products")
    suspend fun createProduct(@Body request: ProductUpsertRequest): Product

    @PUT("api/products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body request: ProductUpsertRequest): Product

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String)

    @GET("api/dishes")
    suspend fun getDishes(
        @Query("category") category: DishCategory? = null,
        @Query("flag") flags: List<ExtraFlag> = emptyList(),
        @Query("query") query: String? = null,
    ): List<Dish>

    @POST("api/dishes")
    suspend fun createDish(@Body request: DishUpsertRequest): Dish

    @PUT("api/dishes/{id}")
    suspend fun updateDish(@Path("id") id: String, @Body request: DishUpsertRequest): Dish

    @DELETE("api/dishes/{id}")
    suspend fun deleteDish(@Path("id") id: String)

    @POST("api/dishes/calculate")
    suspend fun calculateDish(@Body ingredients: List<DishIngredient>): DishCalculationResponse
}

data class ProductUpsertRequest(
    val name: String,
    val photos: List<String> = emptyList(),
    val nutritionPer100g: Nutrition,
    val composition: String? = null,
    val category: ProductCategory,
    val cookingRequirement: CookingRequirement,
    val flags: Set<ExtraFlag> = emptySet(),
)

data class DishUpsertRequest(
    val name: String,
    val photos: List<String> = emptyList(),
    val nutritionPerPortion: Nutrition,
    val ingredients: List<DishIngredient>,
    val portionSizeGrams: Double,
    val category: DishCategory? = null,
    val flags: Set<ExtraFlag> = emptySet(),
)

data class DishCalculationResponse(
    val nutrition: Nutrition,
    val availableFlags: Set<ExtraFlag>,
)

object RecipeBookApiFactory {
    private const val BASE_URL = "http://10.0.2.2:18080/"

    val api: RecipeBookApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecipeBookApi::class.java)
    }
}
