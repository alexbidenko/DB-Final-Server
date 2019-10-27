package tech.crusade.kakaton.product

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.crusade.kakaton.DB
import tech.crusade.kakaton.models.Product

@RestController
@RequestMapping("/api/v1.0")
@CrossOrigin("*")
@Api(value="Оборудование", description="Операции с обслуживающим оборудованием")
class ProductController {

    @PostMapping("/products")
    @ApiOperation(value = "Добавить новое оборудование")
    @ApiResponses(
            value = [
                ApiResponse(code = 201, message = "OK")
            ]
    )
    fun addProduct(@RequestBody product: Product): ResponseEntity<Any> {
        DB.connection.executeUpdate(
                """INSERT INTO products (
                    title, 
                    description,
                    lifetime,
                    grantTime,
                    information
                ) VALUES (
                    '${product.title}',
                    '${product.description}',
                    ${product.lifetime},
                    ${product.grantTime},
                    '${product.information}'
                );""".trimIndent()
        )

        return ResponseEntity(HttpStatus.CREATED)
    }

    @GetMapping("/product")
    @ApiOperation(value = "Список оборудования")
    @ApiResponses(
            value = [
                ApiResponse(code = 200, message = "OK",
                        response = Product::class,
                        responseContainer = "List")
            ]
    )
    fun getProduct(): ResponseEntity<Any> {
        val result = DB.connection.executeQuery("""
            SELECT * FROM products;
        """.trimIndent())

        val response = ArrayList<Product>()

        while (result.next()) {
            response.add(
                    Product.fromSQL(result)
            )
        }
        return ResponseEntity(response, HttpStatus.OK)
    }
}