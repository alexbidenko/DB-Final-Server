package tech.crusade.kakaton.models

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.sql.ResultSet

@ApiModel(description = "Онформация об оборудовании")
class Product (
        val id: Long?,
        val title: String,
        val description: String,
        @ApiModelProperty(notes = "Сколько оно может проработать в милисекундах")
        val lifetime: Long,
        val grantTime: Long,
        val information: String
) {

    companion object {
        fun fromSQL(data: ResultSet): Product {
            return Product(
                    data.getLong("id"),
                    data.getString("title"),
                    data.getString("description"),
                    data.getLong("lifetime"),
                    data.getLong("grantTime"),
                    data.getString("information")
            )
        }
    }
}