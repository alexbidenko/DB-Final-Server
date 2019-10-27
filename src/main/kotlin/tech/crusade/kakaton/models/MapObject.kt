package tech.crusade.kakaton.models

import io.swagger.annotations.ApiModelProperty
import java.sql.ResultSet

class MapObject (
        val id: Long?,
        val type: Long,
        val latitude: Double,
        val longitude: Double,
        @ApiModelProperty(notes = "Timestamp когда поставили оборудование")
        val timeCreate: Long?,
        @ApiModelProperty(notes = "Если есть, уникальный идентификатор оборудования")
        val passport: String?,
        val category: String,
        val coordinates: String
) {

        companion object {
            fun fromDB(data: ResultSet): MapObject {
                return MapObject(
                        data.getLong("id"),
                        data.getLong("type"),
                        data.getDouble("latitude"),
                        data.getDouble("longitude"),
                        data.getLong("timeCreate"),
                        data.getString("passport"),
                        data.getString("category"),
                        data.getString("coordinates")
                )
            }
        }
}