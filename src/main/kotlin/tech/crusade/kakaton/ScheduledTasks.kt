package tech.crusade.kakaton

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.sql.DriverManager

@Component
class ScheduledTasks {

    @Scheduled(cron = "0 0 3 * * *")
    fun checkRenewals() {
        return
    }

    @Scheduled(fixedRate = 1000 * 60 * 5)
    fun reconnectDB() {
        val con = DriverManager.getConnection(StaticData.DBUrl, StaticData.DBUser, StaticData.DBPassword)
        DB.connection = con.createStatement()
    }
}