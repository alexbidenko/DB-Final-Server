package tech.crusade.kakaton

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.sql.DriverManager
import java.sql.Statement

@SpringBootApplication
@EnableScheduling
@EnableSwagger2
class KakatonApplication

// nohup java -jar kakaton-0.0.7.jar 2>&1 > kakaton.log &
// 11740

fun main(args: Array<String>) {
    runApplication<KakatonApplication>(*args)

    val con = DriverManager.getConnection(StaticData.DBUrl, StaticData.DBUser, StaticData.DBPassword)
    DB.connection = con.createStatement()
}

object DB {
    lateinit var connection: Statement
}