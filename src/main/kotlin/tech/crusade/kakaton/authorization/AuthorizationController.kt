package tech.crusade.kakaton.authorization

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*
import tech.crusade.kakaton.DB
import tech.crusade.kakaton.data.PermissionGroups
import tech.crusade.kakaton.models.SingInUser
import tech.crusade.kakaton.models.SingUpUser
import tech.crusade.kakaton.models.User

@RestController
@RequestMapping("/api/v1.0/auth")
@CrossOrigin("*")
@Api(value="Авторизация")
class AuthorizationController {

    @PostMapping("/singUp")
    @ApiOperation(value = "Регистрация")
    @ApiResponses(
            value = [
                ApiResponse(code = 201, message = "OK",
                        response = User::class),
                ApiResponse(code = 409, message = "Почта занята")
            ]
    )
    fun singUp(@RequestBody singUpUser: SingUpUser): ResponseEntity<Any> {
        val checkUser = DB.connection.executeQuery("""
            SELECT * FROM users WHERE email = '${singUpUser.email}';
        """.trimIndent())
        if(checkUser.next()) return ResponseEntity(HttpStatus.CONFLICT)

        val bc = BCryptPasswordEncoder()

        DB.connection.executeUpdate(
                """INSERT INTO users (
                    firstName, 
                    lastName,
                    middleName,
                    email,
                    password,
                    permissionGroup
                ) VALUES (
                    '${singUpUser.firstName}', 
                    '${singUpUser.lastName}', 
                    '${singUpUser.middleName}', 
                    '${singUpUser.email}', 
                    '${bc.encode(singUpUser.password)}',
                    '${PermissionGroups.USER}'
                );""".trimIndent()
        )

        return ResponseEntity(
                User(
                        singUpUser.firstName,
                        singUpUser.lastName,
                        singUpUser.middleName,
                        singUpUser.email,
                        PermissionGroups.USER,
                        createToken(bc, singUpUser.email)
                ), HttpStatus.CREATED)
    }

    @PostMapping("/singIn")
    @ApiOperation(value = "Вход")
    @ApiResponses(
            value = [
                ApiResponse(code = 200, message = "OK",
                        response = User::class),
                ApiResponse(code = 404, message = "Нет пользователя по почте"),
                ApiResponse(code = 403, message = "Пароль не подходит")
            ]
    )
    fun singIn(@RequestBody singInUser: SingInUser): ResponseEntity<Any> {
        val user = DB.connection.executeQuery("SELECT * FROM users WHERE email = '${singInUser.email}';")

        val bc = BCryptPasswordEncoder()
        if(!user.next()) return ResponseEntity(HttpStatus.NOT_FOUND)
        if(!bc.matches(singInUser.password, user.getString("password")))
            return ResponseEntity(HttpStatus.FORBIDDEN)

        return ResponseEntity(
                User(
                        user.getString("firstName"),
                        user.getString("lastName"),
                        user.getString("middleName"),
                        user.getString("email"),
                        user.getString("permissionGroup"),
                        createToken(bc, singInUser.email)
                ),
                HttpStatus.OK
        )
    }

    private fun createToken(bc: BCryptPasswordEncoder, data: String): String {
        val timeCreate = System.currentTimeMillis()
        val token = bc.encode("token_" + data + "_" + timeCreate)
        DB.connection.executeUpdate(
                """INSERT INTO tokens (
                    value, 
                    timeCreate,
                    permissionGroup
                ) VALUES (
                    '$token', 
                    '$timeCreate', 
                    '${PermissionGroups.USER}'
                );""".trimIndent()
        )
        return token
    }
}