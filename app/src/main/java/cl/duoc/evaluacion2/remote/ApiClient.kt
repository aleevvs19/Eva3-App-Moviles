package cl.duoc.evaluacion2.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // Con "adb reverse tcp:8080 tcp:8080" el emulador usa localhost para llegar al PC.
    // Sin adb reverse, en emulador: "http://10.0.2.2:8080/"
    // Teléfono real: IP de tu PC, por ejemplo "http://192.168.1.50:8080/"
    private const val BASE_URL = "http://localhost:8080/"

    @Volatile
    var token: String? = null

    private val cliente = OkHttpClient.Builder()
        .addInterceptor { cadena ->
            val peticion = cadena.request().newBuilder().apply {
                token?.let { addHeader("Authorization", "Bearer $it") }
            }.build()
            cadena.proceed(peticion)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}