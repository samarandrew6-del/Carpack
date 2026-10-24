package pl.statusaut

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: String? = null, val http: Int = 0) : Exception(message)

/** Cookie jar współdzielony z WebView (potrzebny do zabezpieczenia hostingu). */
private class WebViewCookieJar : CookieJar {
    private val manager: CookieManager get() = CookieManager.getInstance()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (c in cookies) manager.setCookie(url.toString(), c.toString())
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val header = manager.getCookie(url.toString()) ?: return emptyList()
        return header.split(";").mapNotNull { Cookie.parse(url, it.trim()) }
    }
}

object Api {
    private lateinit var appContext: Context
    private lateinit var client: OkHttpClient
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun init(context: Context) {
        appContext = context.applicationContext
        // Ten sam User-Agent co w WebView, żeby ciasteczko z testu pasowało do zapytań.
        val ua = WebSettings.getDefaultUserAgent(appContext)
        client = OkHttpClient.Builder()
            .cookieJar(WebViewCookieJar())
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", ua).build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun execute(path: String, method: String, body: JSONObject?): Pair<Int, String> {
        val builder = Request.Builder().url(Config.BASE_URL + path)
        if (body != null) {
            builder.method(method, body.toString().toRequestBody(jsonType))
        } else {
            builder.method(method, null)
        }
        client.newCall(builder.build()).execute().use { response ->
            return response.code to (response.body?.string() ?: "")
        }
    }

    private fun looksLikeHtml(text: String) = text.trimStart().startsWith("<")

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): String {
        var result = withContext(Dispatchers.IO) { execute(path, method, body) }
        if (looksLikeHtml(result.second)) {
            // Zabezpieczenie hostingu: przejdź test w ukrytym WebView i spróbuj jeszcze raz.
            ChallengeSolver.solve(appContext, Config.BASE_URL + path)
            result = withContext(Dispatchers.IO) { execute(path, method, body) }
            if (looksLikeHtml(result.second)) {
                throw ApiException("Serwer zablokował połączenie (zabezpieczenie hostingu).")
            }
        }
        val (code, text) = result
        if (code !in 200..299) {
            val json = runCatching { JSONObject(text) }.getOrNull()
            val message = json?.optString("error").orEmpty().ifEmpty { "Błąd serwera ($code)" }
            val errorCode = json?.optString("code").orEmpty().ifEmpty { null }
            throw ApiException(message, errorCode, code)
        }
        return text
    }

    private fun parseCars(text: String): List<Car> {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) return emptyList() // PHP zwraca [] gdy nie ma aut
        val root = JSONObject(trimmed)
        val cars = mutableListOf<Car>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val id = keys.next()
            val o = root.getJSONObject(id)
            cars += Car(
                id = id,
                name = o.optString("name"),
                plate = o.optString("plate"),
                occupied = o.optBoolean("occupied"),
                by = o.optString("by"),
                phone = o.optString("phone"),
                time = o.optString("time"),
                caretaker = o.optString("caretaker")
            )
        }
        return cars
    }

    suspend fun getCars(): List<Car> = parseCars(call("api.php"))

    suspend fun take(carId: String, name: String, phone: String, code: String, boardCode: String): List<Car> {
        val body = JSONObject()
            .put("id", carId).put("action", "take")
            .put("by", name).put("phone", phone).put("code", code)
            .put("board_code", boardCode)
        return parseCars(call("api.php", "POST", body))
    }

    suspend fun free(carId: String, code: String, boardCode: String): List<Car> {
        val body = JSONObject()
            .put("id", carId).put("action", "free")
            .put("code", code).put("board_code", boardCode)
        return parseCars(call("api.php", "POST", body))
    }
}
