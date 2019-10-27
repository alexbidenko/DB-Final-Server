package tech.crusade.kakaton.map

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.crusade.kakaton.DB
import tech.crusade.kakaton.models.MapObject

@RestController
@RequestMapping("/api/v1.0/map")
@CrossOrigin("*")
@Api(value="Оборудование для карты")
class MapController {

    @PostMapping("/add")
    @ApiOperation(value = "Добавить новое оборудование на карту")
    @ApiResponses(
            value = [
                ApiResponse(code = 200, message = "Оборудование добавлено, но есть похожее"),
                ApiResponse(code = 201, message = "Оборудование добавлено"),
                ApiResponse(code = 409, message = "Есть прибор с таким же идентификатором")
            ]
    )
    fun addToMap(@RequestBody mapObject: MapObject): ResponseEntity<Any> {
        if(mapObject.passport != null && mapObject.passport != "") {
            val checkMapObject = DB.connection.executeQuery("""
                SELECT * FROM mapObjects WHERE passport = '${mapObject.passport}';
            """.trimIndent())
            if (checkMapObject.next()) return ResponseEntity(HttpStatus.CONFLICT)
        }

        val checkMapObject = DB.connection.executeQuery(""" 
            SELECT * FROM mapObjects WHERE
            latitude < ${mapObject.latitude + mapDelta} AND
            latitude > ${mapObject.latitude - mapDelta} AND
            longitude < ${mapObject.longitude + mapDelta} AND
            longitude > ${mapObject.longitude - mapDelta}';
        """.trimIndent())

        DB.connection.executeUpdate(
                """INSERT INTO mapObjects (
                    type, 
                    latitude,
                    longitude,
                    timeCreate,
                    passport,
                    category,
                    coordinates
                ) VALUES (
                    ${mapObject.type}, 
                    ${mapObject.latitude}, 
                    ${mapObject.longitude},
                    ${System.currentTimeMillis()},
                    '${mapObject.passport ?: ""}',
                    ${mapObject.category},
                    ${mapObject.coordinates},
                );""".trimIndent()
        )

        val newMapObject = DB.connection.executeQuery("SELECT * FROM mapObjects DESC LIMIT 1")
        newMapObject.next()
        val objectData = DB.connection.executeQuery("SELECT * FROM products WHERE id = ${mapObject.type}")
        objectData.next()
        DB.connection.executeUpdate(
                """INSERT INTO renewals (
                    idObject,
                    latitude,
                    longitude,
                    timeRenewal
                ) VALUES (
                    ${newMapObject.getLong("id")},
                    ${mapObject.latitude},
                    ${mapObject.longitude},
                    ${System.currentTimeMillis() + objectData.getLong("lifetime")}
                );""".trimIndent()
        )

        return if (checkMapObject.next())
            ResponseEntity(MapObject.fromDB(checkMapObject), HttpStatus.OK)
        else ResponseEntity(HttpStatus.CREATED)
    }

    @PostMapping("/check")
    @ApiOperation(value = "Проверка места на возможность прокладки там чего-нибудь")
    @ApiResponses(
            value = [
                ApiResponse(code = 200, message = "Конфликтного оборудования не найдено"),
                ApiResponse(code = 409, message = "Найдено конфликтное оборудование")
            ]
    )
    fun checkToMap(@RequestBody mapObject: MapObject): ResponseEntity<Any> {
        val checkMapObjects = DB.connection.executeQuery(""" 
            SELECT * FROM mapObjects WHERE
            latitude < ${mapObject.latitude + mapCheckDelta} AND
            latitude > ${mapObject.latitude - mapCheckDelta} AND
            longitude < ${mapObject.longitude + mapCheckDelta} AND
            longitude > ${mapObject.longitude - mapCheckDelta}';
        """.trimIndent())

        var isConflict = false
        var cashCheckMapObject: JsonNode? = null
        while (checkMapObjects.next()) {
            val checkMapObject = ObjectMapper().readTree(checkMapObjects.getString("coordinates"))
            val originMapObject = ObjectMapper().readTree(mapObject.coordinates)
            for(i in 0 until checkMapObject.size()) {
                if(cashCheckMapObject == null) {
                    cashCheckMapObject = checkMapObject[0]
                } else {
                    if(getIsCross(
                                    originMapObject[0]["lat"].asDouble(),
                                    originMapObject[0]["lng"].asDouble(),
                                    originMapObject[1]["lat"].asDouble(),
                                    originMapObject[1]["lng"].asDouble(),
                                    cashCheckMapObject["lat"].asDouble(),
                                    cashCheckMapObject["lng"].asDouble(),
                                    checkMapObject[i]["lat"].asDouble(),
                                    checkMapObject[i]["lng"].asDouble()
                            )
                    ) isConflict = true
                    cashCheckMapObject = checkMapObject[i]
                }
            }
        }
        return if(isConflict) ResponseEntity(mapOf("isCross" to isConflict), HttpStatus.CONFLICT)
        else ResponseEntity(mapOf("isCross" to isConflict), HttpStatus.OK)
    }

    @GetMapping("/list")
    @ApiOperation(value = "Список оборудования")
    @ApiResponses(
            value = [
                ApiResponse(code = 200, message = "Список оборудования в зоне",
                        response = MapObject::class,
                        responseContainer = "List")
            ]
    )
    fun getMapObjects(
            @RequestParam("latitude") latitude: Double,
            @RequestParam("longitude") longitude: Double,
            @RequestParam("radius") radius: Double,
            @RequestParam("type", required = false) type: Long?
    ): ResponseEntity<Any> {
        val mapObjects = DB.connection.executeQuery(""" 
            SELECT * FROM mapObjects WHERE
            latitude < ${latitude + radius} AND
            latitude > ${latitude - radius} AND
            longitude < ${longitude + radius} AND
            longitude > ${longitude - radius}';
        """.trimIndent())

        val result = ArrayList<MapObject>()
        while (mapObjects.next()) {
            result.add(MapObject.fromDB(mapObjects))
        }

        return ResponseEntity(result, HttpStatus.OK)
    }

    companion object {
        private const val mapDelta = 0.0001
        private const val mapCheckDelta = 0.01

        private fun getIsCross(
                x11: Double, y11: Double, x12: Double, y12: Double,
                x21: Double, y21: Double, x22: Double, y22: Double
        ): Boolean {
            val a1 = y11 - y12
            val b1 = x12 - x11
            val a2 = y21 - y22
            val b2 = x22 - x21

            val d = a1 * b2 - a2 * b1;
            return if(d != 0.0) {
                val c1 = y12 * x11 - x12 * y11
                val c2 = y22 * x21 - x22 * y21

                val xi = (b1 * c2 - b2 * c1) / d
                val yi = (a2 * c1 - a1 * c2) / d

                isIn(x11, x12, xi) &&
                isIn(x21, x22, xi) &&
                isIn(y11, y12, yi) &&
                isIn(y21, y22, yi)
            } else false
        }

        private fun isIn(x1: Double, x2: Double, xi: Double): Boolean {
            return (x1 > x2 && xi < x1 && xi > x2) ||
            (x1 < x2 && xi > x1 && xi < x2)
        }
    }
}