package cl.duoc.evaluacion2.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class LoginRequest(
    val correo: String,
    val password: String,
    val perfil: String
)

data class LoginResponse(
    val token: String,
    val perfil: String,
    val nombre: String
)

data class ContenidoDto(
    val id: Long,
    val asignaturaId: String,
    val unidad: String,
    val codigoOA: String?,
    val descripcionOA: String,
    val explicacion: String,
    val tituloActividad: String,
    val pregunta: String,
    val estado: String,
    val observacion: String?,
    val autorCorreo: String?,
    val autorPerfil: String?,
    val actualizado: String?
)

data class ContenidoRequest(
    val asignaturaId: String,
    val unidad: String,
    val codigoOA: String,
    val descripcionOA: String,
    val explicacion: String,
    val tituloActividad: String,
    val pregunta: String
)

data class CambioEstadoRequest(
    val accion: String,
    val observacion: String?
)

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body datos: LoginRequest): LoginResponse

    @GET("api/contenidos")
    suspend fun contenidos(): List<ContenidoDto>

    @POST("api/contenidos")
    suspend fun crearContenido(@Body datos: ContenidoRequest): ContenidoDto

    @PUT("api/contenidos/{id}")
    suspend fun editarContenido(
        @Path("id") id: Long,
        @Body datos: ContenidoRequest
    ): ContenidoDto

    @PUT("api/contenidos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body datos: CambioEstadoRequest
    ): ContenidoDto
}