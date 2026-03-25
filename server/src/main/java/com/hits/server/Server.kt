package com.hits.recipebook.server

import com.hits.server.CookingRequirement
import com.hits.server.DeletionBlockedResponse
import com.hits.server.Dish
import com.hits.server.DishCategory
import com.hits.server.DishFilter
import com.hits.server.DishIngredient
import com.hits.server.ExtraFlag
import com.hits.server.Product
import com.hits.server.ProductCategory
import com.hits.server.ProductFilter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        recipeBookModule(RecipeService(RecipeRepository()))
    }.start(wait = true)
}

fun Application.recipeBookModule(service: RecipeService) {
    install(CallLogging)
    install(ContentNegotiation) { json(Json { prettyPrint = true; ignoreUnknownKeys = true }) }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Bad request")))
        }
        exception<NoSuchElementException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to (cause.message ?: "Not found")))
        }
        exception<ProductDeletionBlockedException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                DeletionBlockedResponse("Product is used by dishes", cause.dishIds)
            )
        }
    }

    routing {
        route("/api/products") {
            get {
                val flags = call.request.queryParameters.getAll("flag")
                    ?.mapNotNull { runCatching { ExtraFlag.valueOf(it) }.getOrNull() }
                    ?.toSet().orEmpty()
                val items = service.listProducts(
                    ProductFilter(
                        category = call.request.queryParameters["category"]?.let {
                            ProductCategory.valueOf(
                                it
                            )
                        },
                        cookingRequirement = call.request.queryParameters["cookingRequirement"]?.let {
                            CookingRequirement.valueOf(
                                it
                            )
                        },
                        flags = flags,
                        query = call.request.queryParameters["query"],
                        sortBy = call.request.queryParameters["sortBy"] ?: "name"
                    )
                )
                call.respond(items)
            }
            post {
                val body = call.receive<Product>()
                call.respond(HttpStatusCode.Created, service.createProduct(body))
            }
            get("/{id}") { call.respond(service.product(call.parameters["id"]!!)) }
            put("/{id}") { call.respond(service.updateProduct(call.parameters["id"]!!, call.receive())) }
            delete("/{id}") {
                service.deleteProduct(call.parameters["id"]!!)
                call.respond(HttpStatusCode.NoContent)
            }
        }

        route("/api/dishes") {
            get {
                val flags = call.request.queryParameters.getAll("flag")
                    ?.mapNotNull { runCatching { ExtraFlag.valueOf(it) }.getOrNull() }
                    ?.toSet().orEmpty()
                call.respond(
                    service.listDishes(
                        DishFilter(
                            category = call.request.queryParameters["category"]?.let {
                                DishCategory.valueOf(
                                    it
                                )
                            },
                            flags = flags,
                            query = call.request.queryParameters["query"],
                        )
                    )
                )
            }
            post {
                val body = call.receive<Dish>()
                call.respond(HttpStatusCode.Created, service.createDish(body))
            }
            get("/{id}") { call.respond(service.dish(call.parameters["id"]!!)) }
            put("/{id}") { call.respond(service.updateDish(call.parameters["id"]!!, call.receive())) }
            delete("/{id}") {
                service.deleteDish(call.parameters["id"]!!)
                call.respond(HttpStatusCode.NoContent)
            }
            post("/calculate") {
                val ingredients = call.receive<List<DishIngredient>>()
                call.respond(
                    mapOf(
                        "nutrition" to service.calculateNutrition(ingredients),
                        "availableFlags" to service.availableFlags(ingredients)
                    )
                )
            }
        }
    }
}
