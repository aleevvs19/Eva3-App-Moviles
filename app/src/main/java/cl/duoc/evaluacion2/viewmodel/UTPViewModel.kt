package cl.duoc.evaluacion2.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.evaluacion2.remote.ApiClient
import cl.duoc.evaluacion2.remote.CambioEstadoRequest
import cl.duoc.evaluacion2.remote.ContenidoDto
import cl.duoc.evaluacion2.remote.ContenidoRequest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class EstadoUTP(val etiqueta: String) {
    BORRADOR("Borrador"),
    EN_REVISION("En revisión"),
    AUTORIZADO("Autorizado"),
    RECHAZADO("Rechazado"),
    PUBLICADO("Publicado"),
    DESHABILITADO("Deshabilitado")
}

data class DatosContenidoUTP(
    val asignaturaId: String,
    val unidad: String,
    val codigoOA: String,
    val descripcionOA: String,
    val explicacion: String,
    val tituloActividad: String,
    val pregunta: String
)

data class ContenidoUTP(
    val id: Long,
    val datos: DatosContenidoUTP,
    val estado: EstadoUTP = EstadoUTP.BORRADOR,
    val observacion: String = "",
    val actualizado: String = ""
)

data class RegistroUTP(
    val contenidoId: Long,
    val accion: String,
    val fecha: String
)

class UTPViewModel(context: Context) : ViewModel() {

    private val preferencias =
        context.applicationContext.getSharedPreferences(
            "mpazpro_utp_demo",
            Context.MODE_PRIVATE
        )

    var contenidos by mutableStateOf<List<ContenidoUTP>>(emptyList())
        private set

    var bitacora by mutableStateOf(leerBitacora())
        private set

    var errorRed by mutableStateOf("")
        private set

    // ---------- Lectura desde el backend ----------

    fun cargar() {
        viewModelScope.launch {
            try {
                recargar()
            } catch (e: Exception) {
                errorRed = mensajeError(e)
            }
        }
    }

    private suspend fun recargar() {
        contenidos = ApiClient.api.contenidos().map { it.aContenidoUTP() }
        errorRed = ""
    }

    private fun ContenidoDto.aContenidoUTP() = ContenidoUTP(
        id = id,
        datos = DatosContenidoUTP(
            asignaturaId = asignaturaId,
            unidad = unidad,
            codigoOA = codigoOA ?: "",
            descripcionOA = descripcionOA,
            explicacion = explicacion,
            tituloActividad = tituloActividad,
            pregunta = pregunta
        ),
        estado = EstadoUTP.values().firstOrNull { it.name == estado }
            ?: EstadoUTP.BORRADOR,
        observacion = observacion ?: "",
        actualizado = actualizado ?: ""
    )

    private fun mensajeError(e: Exception): String = when (e) {
        is HttpException -> "Error del servidor (${e.code()})"
        is IOException -> "Sin conexión con el servidor"
        else -> "No se pudo completar la operación"
    }

    // ---------- Bitácora (local por ahora) ----------

    private fun fechaActual(): String {
        return LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
        )
    }

    private fun leerBitacora(): List<RegistroUTP> {
        return try {
            val array = JSONArray(
                preferencias.getString("bitacora", "[]") ?: "[]"
            )

            (0 until array.length()).mapNotNull { i ->
                val json = array.optJSONObject(i)
                    ?: return@mapNotNull null

                RegistroUTP(
                    contenidoId = json.optLong("contenidoId"),
                    accion = json.optString("accion"),
                    fecha = json.optString("fecha")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun registrar(id: Long, accion: String) {
        bitacora = listOf(
            RegistroUTP(id, accion, fechaActual())
        ) + bitacora

        val array = JSONArray()

        bitacora.forEach { registro ->
            array.put(
                JSONObject()
                    .put("contenidoId", registro.contenidoId)
                    .put("accion", registro.accion)
                    .put("fecha", registro.fecha)
            )
        }

        preferencias.edit()
            .putString("bitacora", array.toString())
            .apply()
    }

    // ---------- Escritura hacia el backend ----------

    private fun ejecutar(
        textoBitacora: String,
        operacion: suspend () -> Long
    ) {
        viewModelScope.launch {
            try {
                val id = operacion()
                registrar(id, textoBitacora)
                recargar()
            } catch (e: Exception) {
                errorRed = mensajeError(e)
            }
        }
    }

    fun guardarContenido(
        idEditar: Long?,
        datos: DatosContenidoUTP
    ): Boolean {

        val limpio = datos.copy(
            unidad = datos.unidad.trim(),
            codigoOA = datos.codigoOA.trim(),
            descripcionOA = datos.descripcionOA.trim(),
            explicacion = datos.explicacion.trim(),
            tituloActividad = datos.tituloActividad.trim(),
            pregunta = datos.pregunta.trim()
        )

        val asignaturas = setOf(
            "lenguaje",
            "matematica",
            "ciencias",
            "historia"
        )

        if (limpio.asignaturaId !in asignaturas ||
            limpio.unidad.isBlank() ||
            limpio.descripcionOA.isBlank() ||
            limpio.explicacion.isBlank() ||
            limpio.tituloActividad.isBlank() ||
            limpio.pregunta.isBlank()
        ) {
            return false
        }

        val peticion = ContenidoRequest(
            asignaturaId = limpio.asignaturaId,
            unidad = limpio.unidad,
            codigoOA = limpio.codigoOA,
            descripcionOA = limpio.descripcionOA,
            explicacion = limpio.explicacion,
            tituloActividad = limpio.tituloActividad,
            pregunta = limpio.pregunta
        )

        if (idEditar == null) {

            ejecutar("Contenido creado como borrador") {
                ApiClient.api.crearContenido(peticion).id
            }

        } else {

            val anterior = contenidos.firstOrNull {
                it.id == idEditar
            } ?: return false

            if (anterior.estado != EstadoUTP.BORRADOR &&
                anterior.estado != EstadoUTP.RECHAZADO
            ) {
                return false
            }

            ejecutar("Borrador modificado") {
                ApiClient.api.editarContenido(idEditar, peticion).id
            }
        }

        return true
    }

    private fun cambiarEstado(
        id: Long,
        estadosPermitidos: Set<EstadoUTP>,
        nuevoEstado: EstadoUTP,
        accionApi: String,
        observacion: String = ""
    ): Boolean {

        val anterior = contenidos.firstOrNull {
            it.id == id
        } ?: return false

        if (anterior.estado !in estadosPermitidos) {
            return false
        }

        if (nuevoEstado == EstadoUTP.RECHAZADO &&
            observacion.isBlank()
        ) {
            return false
        }

        ejecutar("Estado cambiado a: ${nuevoEstado.etiqueta}") {
            ApiClient.api.cambiarEstado(
                id,
                CambioEstadoRequest(accionApi, observacion.trim())
            ).id
        }

        return true
    }

    fun enviarRevision(id: Long): Boolean {
        return cambiarEstado(
            id,
            setOf(EstadoUTP.BORRADOR),
            EstadoUTP.EN_REVISION,
            "enviar-revision"
        )
    }

    fun aprobar(id: Long): Boolean {
        return cambiarEstado(
            id,
            setOf(EstadoUTP.EN_REVISION),
            EstadoUTP.AUTORIZADO,
            "aprobar"
        )
    }

    fun rechazar(
        id: Long,
        observacion: String
    ): Boolean {
        return cambiarEstado(
            id,
            setOf(EstadoUTP.EN_REVISION),
            EstadoUTP.RECHAZADO,
            "rechazar",
            observacion
        )
    }

    fun publicar(id: Long): Boolean {
        return cambiarEstado(
            id,
            setOf(EstadoUTP.AUTORIZADO),
            EstadoUTP.PUBLICADO,
            "publicar"
        )
    }

    fun deshabilitar(id: Long): Boolean {
        return cambiarEstado(
            id,
            setOf(EstadoUTP.PUBLICADO),
            EstadoUTP.DESHABILITADO,
            "deshabilitar"
        )
    }
}

class UTPViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (!modelClass.isAssignableFrom(
                UTPViewModel::class.java
            )
        ) {
            throw IllegalArgumentException(
                "ViewModel no reconocido"
            )
        }

        return UTPViewModel(context) as T
    }
}