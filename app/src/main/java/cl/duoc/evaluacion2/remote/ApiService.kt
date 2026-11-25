package cl.duoc.evaluacion2.remote

import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val correo: String, val password: String)
data class LoginResponse(val token: String, val perfil: String, val nombre: String)

interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body datos: LoginRequest): LoginResponse
}