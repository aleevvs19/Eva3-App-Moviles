package cl.duoc.evaluacion2.session

import cl.duoc.evaluacion2.remote.ApiClient

object Sesion {

    var perfil: String? = null
        private set

    var nombre: String? = null
        private set

    fun iniciar(perfil: String, nombre: String) {
        this.perfil = perfil
        this.nombre = nombre
    }

    fun cerrar() {
        perfil = null
        nombre = null
        ApiClient.token = null
    }

    fun esRutaPublica(ruta: String): Boolean =
        ruta == "inicio" || ruta == "registro" || ruta.startsWith("login/")

    fun puedeEntrar(ruta: String): Boolean {
        val perfilDueno = when {
            ruta == "estudiante" ||
                    ruta.startsWith("actividad") ||
                    ruta.startsWith("prueba-final") -> "estudiante"

            ruta == "docente" ||
                    ruta == "configuracion-prueba" ||
                    ruta == "informe-pedagogico" -> "docente"

            ruta == "utp" ||
                    ruta == "gestion-unidades" -> "utp"

            else -> return false
        }
        return perfil == perfilDueno
    }
}