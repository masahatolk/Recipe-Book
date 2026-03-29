package com.hits.recipebook.network

import android.os.Build
import com.hits.recipebook.CookingRequirement
import com.hits.recipebook.Dish
import com.hits.recipebook.DishCategory
import com.hits.recipebook.DishIngredient
import com.hits.recipebook.ExtraFlag
import com.hits.recipebook.Nutrition
import com.hits.recipebook.Product
import com.hits.recipebook.ProductCategory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
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

    @Multipart
    @POST("api/photos")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): PhotoUploadResponse
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

data class PhotoUploadResponse(
    val url: String,
)
object RecipeBookApiFactory {
    private const val EMULATOR_HOST = "10.0.2.2"
    private const val DEVICE_HOST = "127.0.0.1"
    private const val API_PORT = 18080

    private fun isRunningOnEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT
    }

    /**
     * Для физического устройства пробрасывайте порт:
     * adb reverse tcp:18080 tcp:18080
     *
     * Если backend запущен в Docker c пробросом "18080:8080", то на хосте
     * доступен именно 18080, поэтому в adb reverse справа тоже 18080.
     */
    val BASE_URL: String by lazy {
        val host = if (isRunningOnEmulator()) EMULATOR_HOST else DEVICE_HOST
        "http://$host:$API_PORT/"
    }

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
