package tech.crusade.kakaton.renewal

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import tech.crusade.kakaton.DB
import tech.crusade.kakaton.models.Renewal

@RestController
@RequestMapping("/api/v1.0")
@CrossOrigin("*")
class RenewalsController {

    @GetMapping("/renewal")
    fun getRenewals(
            @RequestParam("latitude") latitude: Double,
            @RequestParam("longitude") longitude: Double,
            @RequestParam("radius") radius: Double
    ): ResponseEntity<Any> {
        val renewals = DB.connection.executeQuery(""" 
            SELECT * FROM renewals WHERE
            latitude < ${latitude + radius} AND
            latitude > ${latitude - radius} AND
            longitude < ${longitude + radius} AND
            longitude > ${longitude - radius} AND
            timeRenewal < ${System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7};
        """.trimIndent())

        val result = ArrayList<Renewal>()
        while (renewals.next()) {
            result.add(Renewal(
                    renewals.getLong("id"),
                    renewals.getLong("idObject"),
                    renewals.getDouble("latitude"),
                    renewals.getDouble("longitude"),
                    renewals.getLong("timeRenewal")
            ))
        }

        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/renewal/{idObject}")
    fun addBrokenObject(@PathVariable("idObject") idObject: Long) {
        val newMapObject = DB.connection.executeQuery("SELECT * FROM mapObjects WHERE id = $idObject")
        newMapObject.next()
        DB.connection.executeUpdate(
                """INSERT INTO renewals (
                    idObject,
                    latitude,
                    longitude,
                    timeRenewal
                ) VALUES (
                    ${newMapObject.getLong("id")},
                    ${newMapObject.getString("latitude")},
                    ${newMapObject.getString("longitude")},
                    ${-System.currentTimeMillis()}
                );""".trimIndent()
        )
    }
}