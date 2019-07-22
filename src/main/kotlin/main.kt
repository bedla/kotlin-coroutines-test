import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.random.Random
import kotlin.system.measureTimeMillis

const val asyncIO = true

val idRegex = """"([^"]*)"\s*:\s*"([^"]*)"""".toRegex()

fun main() {
    measureTimeMillis {
        runBlocking {
            val idUrls = (1..10).toList()
                .map { upload("""{"value": $it}""") }

            for ((idx, idUrl) in idUrls.withIndex()) {
                async(if (asyncIO) Dispatchers.IO else EmptyCoroutineContext) {
                    download(idUrl, idx)
                }
            }
        }
    }.also { println("${Duration.ofMillis(it)}") }
}

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun upload(payload: String): String {
    val url = URL("""http://www.mocky.io/""")
    val httpCon = url.openConnection() as HttpURLConnection
    httpCon.doOutput = true
    httpCon.requestMethod = "POST"

    val form = mapOf(
        "statuscode" to "200",
        "location" to "",
        "contenttype" to "application/json",
        "charset" to "UTF-8",
        "body" to payload
    )

    OutputStreamWriter(httpCon.outputStream).use {
        it.write(form.asSequence()
            .map { entry -> "${entry.key.urlEncode()}=${entry.value.urlEncode()}" }
            .joinToString("&"))
    }

    val response = httpCon.inputStream.use {
        it.readBytes().toString(Charsets.UTF_8)
    }

    val result = idRegex.find(response) ?: error("unable to find url in $response")
    return result.groups[2]?.value ?: error("group 2 not found in $result")
}

fun download(url: String, idx: Int): Unit {
    val randomBytes = Random.nextBytes(128)
    log("download start: $idx")
    URL("""$url?mocky-delay=1000ms""").openStream().use {
        it.readBytes()
    }
    log("download end: $idx")
}

fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
