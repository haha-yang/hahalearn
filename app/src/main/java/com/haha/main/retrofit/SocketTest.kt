package com.haha.main.retrofit

import android.os.Build
import com.google.gson.Gson
import com.haha.log.DOFLogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 *
 * @author:     haha
 * @date:       2026/8/30
 * Description: 裸 Socket 对照 Demo：TCP 三次握手 + TLS + HTTP/1.1，以及 UDP DNS（无握手）。
 *              HTTP/2、HTTP/3 见 HttpsVersionTest；协议对比见 docs/https-http-versions.md
 *
 **/
class SocketTest {
    private val TAG = this.javaClass.simpleName
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun test() {
        // 对应仓库：https://github.com/haha-yang/hahalearn
        httpsGet("https://api.github.com/repos/haha-yang/hahalearn")
        httpsPost("https://httpbin.org/post")
        // 8.8.8.8 在国内常被拦；优先用国内公共 DNS，失败再回退
        udpDns("api.github.com")
        // HTTP/2、HTTP/3 见同包 HttpsVersionTest（由 TestLearnUtils 触发）
    }

    fun httpsGet(url: String) {
        scope.launch {
            try {
                val body = httpsRequest(method = "GET", url = url, body = null)
                val repo = Gson().fromJson(body, GithubRepoBean::class.java)
                DOFLogUtil.d(
                    TAG,
                    "httpsGet repo id=${repo.id} fullName=${repo.fullName} " +
                            "owner=${repo.owner?.login} language=${repo.language} " +
                            "stars=${repo.stargazersCount} defaultBranch=${repo.defaultBranch} " +
                            "bean=$repo",
                )
            } catch (e: Exception) {
                DOFLogUtil.e(TAG, "httpsGet onFailure: ${e.message}", e)
            }
        }
    }

    fun httpsPost(url: String) {
        scope.launch {
            try {
                val form = "city=${URLEncoder.encode("长沙", "UTF-8")}&name=${
                    URLEncoder.encode(
                        "haha",
                        "UTF-8"
                    )
                }"
                val body = httpsRequest(
                    method = "POST",
                    url = url,
                    body = form.toByteArray(StandardCharsets.UTF_8),
                    contentType = "application/x-www-form-urlencoded; charset=utf-8",
                )
                DOFLogUtil.d(TAG, "httpsPost result: $body")
            } catch (e: Exception) {
                DOFLogUtil.e(TAG, "httpsPost onFailure: ${e.message}", e)
            }
        }
    }

    /**
     * UDP 无连接：send 不握手，对端不在 / 丢包发送方默认无感知，超时才失败。
     * 这里发一条 DNS A 查询（53 端口），对照上面 HTTPS 走的 TCP。
     * 默认依次尝试阿里 / DNSPod / 114 / Google，避免单点（如 8.8.8.8）超时。
     */
    fun udpDns(host: String, dnsServers: List<String> = DEFAULT_DNS_SERVERS) {
        scope.launch {
            var lastError: Exception? = null
            for (dnsServer in dnsServers) {
                try {
                    DatagramSocket().use { ds ->
                        ds.soTimeout = UDP_DNS_TIMEOUT_MS
                        val id = (System.currentTimeMillis() and 0xFFFF).toInt()
                        val query = buildDnsQuery(host, id)
                        val server = InetAddress.getByName(dnsServer)
                        ds.send(DatagramPacket(query, query.size, server, 53))
                        DOFLogUtil.d(
                            TAG,
                            "udpDns send $host A? -> $dnsServer:53, ${query.size} bytes, 无 TCP 三次握手",
                        )

                        val buf = ByteArray(512)
                        val incoming = DatagramPacket(buf, buf.size)
                        ds.receive(incoming)
                        val ips = parseDnsARecords(incoming.data, incoming.length)
                        DOFLogUtil.d(
                            TAG,
                            "udpDns recv from $dnsServer, ${incoming.length} bytes, A=$ips"
                        )
                        return@launch
                    }
                } catch (e: Exception) {
                    lastError = e
                    DOFLogUtil.w(TAG, "udpDns $dnsServer failed: ${e.message}, try next")
                }
            }
            DOFLogUtil.e(TAG, "udpDns onFailure: all servers failed", lastError)
        }
    }

    /**
     * ① DNS → ② TCP connect（内核三次握手）→ ③ TLS → ④ 写 HTTP → ⑤ 读响应。
     * 强制 ALPN `http/1.1`，因为本 Demo 不会说 HTTP/2 帧。
     */
    private fun httpsRequest(
        method: String,
        url: String,
        body: ByteArray?,
        contentType: String? = null,
    ): String {
        val uri = URI(url)
        require(uri.scheme.equals("https", ignoreCase = true)) { "只演示 https，url=$url" }
        val host = requireNotNull(uri.host) { "url 无 host: $url" }
        val port = if (uri.port == -1) 443 else uri.port
        val path = buildPath(uri)
        DOFLogUtil.d(TAG, "uri = $uri, host = $host, port = $port, path = $path")

        // ① DNS：Socket 只能连 IP。系统解析多数走 UDP 53。
        val addresses = InetAddress.getAllByName(host)
        DOFLogUtil.d(TAG, "[1] DNS $host -> ${addresses.joinToString { it.hostAddress ?: "?" }}")
        val address = addresses.first()

        Socket().use { tcp ->
            tcp.soTimeout = READ_TIMEOUT_MS
            // ② TCP：connect() 阻塞到三次握手完成（SYN / SYN-ACK / ACK）或超时。
            DOFLogUtil.d(TAG, "[2] TCP connect start ${address.hostAddress}:$port （三次握手）")
            tcp.connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS)
            DOFLogUtil.d(
                TAG,
                "[2] TCP ESTABLISHED local=${tcp.localSocketAddress} remote=${tcp.remoteSocketAddress}",
            )

            val ssl = wrapTls(tcp, host, port)
            ssl.use {
                // ③ TLS：证书校验 + 密钥协商。失败时还没写出任何 HTTP。
                ssl.startHandshake()
                val session = ssl.session
                DOFLogUtil.d(
                    TAG,
                    "[3] TLS ${session.protocol} ${session.cipherSuite} peer=${session.peerHost}"
                )

                val header = buildString {
                    append("$method $path HTTP/1.1\r\n")
                    append("Host: $host\r\n")
                    append("Accept: application/json\r\n")
                    append("User-Agent: HahaLearn-SocketTest\r\n")
                    append("Connection: close\r\n")
                    if (body != null) {
                        append("Content-Type: ${contentType ?: "application/octet-stream"}\r\n")
                        append("Content-Length: ${body.size}\r\n")
                    }
                    append("\r\n")
                }.toByteArray(StandardCharsets.US_ASCII)

                val output = BufferedOutputStream(ssl.outputStream)
                output.write(header)
                if (body != null) output.write(body)
                output.flush()
                DOFLogUtil.d(
                    TAG,
                    "[4] write $method $path, header=${header.size} body=${body?.size ?: 0}"
                )

                val input = BufferedInputStream(ssl.inputStream)
                val (code, responseBody) = readHttpResponse(input)
                DOFLogUtil.d(TAG, "[5] HTTP $code, body ${responseBody.length} chars")
                if (code !in 200..299) {
                    throw IllegalStateException("http $code, body=$responseBody")
                }
                return responseBody
            }
        }
    }

    private fun wrapTls(tcp: Socket, host: String, port: Int): SSLSocket {
        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val ssl = factory.createSocket(tcp, host, port, true) as SSLSocket
        ssl.soTimeout = READ_TIMEOUT_MS
        val params = ssl.sslParameters
        params.endpointIdentificationAlgorithm = "HTTPS"
        params.serverNames = listOf(SNIHostName(host))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            params.applicationProtocols = arrayOf("http/1.1")
        }
        ssl.sslParameters = params
        return ssl
    }

    private fun buildPath(uri: URI): String {
        val path = uri.rawPath.orEmpty().ifEmpty { "/" }
        val query = uri.rawQuery
        return if (query.isNullOrEmpty()) path else "$path?$query"
    }

    private fun readHttpResponse(input: InputStream): Pair<Int, String> {
        val headerText = readHeaderBlock(input)
        val statusLine = headerText.substringBefore("\r\n")
        val code = statusLine.split(' ').getOrNull(1)?.toIntOrNull() ?: -1
        val headers = parseHeaders(headerText)
        val encoding = headers["transfer-encoding"].orEmpty()
        DOFLogUtil.d(
            TAG, "headerText = $headerText\n " +
                    "statusLine = $statusLine\n " +
                    "code = $code\n " +
                    "headers = $headers\n " +
                    "encoding = $encoding"
        )
        val bodyBytes = when {
            encoding.contains("chunked", ignoreCase = true) -> readChunked(input)
            headers["content-length"] != null -> {
                val len = headers["content-length"]!!.trim().toInt()
                readFully(input, len)
            }

            else -> input.readBytes()
        }
        val charset = charsetFromContentType(headers["content-type"])
        return code to String(bodyBytes, charset)
    }

    private fun readHeaderBlock(input: InputStream): String {
        val out = ByteArrayOutputStream()
        var matched = 0
        // 这是在字节里写 HTTP 头结束标记：连续两个 CRLF，也就是 \r\n\r\n。
        val crlfCrlf = byteArrayOf(0x0d, 0x0a, 0x0d, 0x0a)
        while (matched < 4) {
            val b = input.read()
            if (b < 0) break
            out.write(b)
            matched = if (b == crlfCrlf[matched].toInt()) matched + 1 else 0
        }
        return out.toString(StandardCharsets.ISO_8859_1.name())
    }

    private fun parseHeaders(headerBlock: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        // drop(n): 丢掉前 n 个元素，后面的留下
        // return@forEach: 相当于continue
        headerBlock.split("\r\n").drop(1).forEach { line ->
            if (line.isEmpty()) return@forEach
            val idx = line.indexOf(':')
            if (idx <= 0) return@forEach
            // take() 类似 substring(0, idx)
            map[line.take(idx).trim().lowercase()] = line.substring(idx + 1).trim()
        }
        return map
    }

    private fun readChunked(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val sizeLine = readAsciiLine(input).substringBefore(';').trim()
            val size = sizeLine.toInt(16)
            if (size == 0) {
                while (readAsciiLine(input).isNotEmpty()) {
                    // trailing headers
                }
                break
            }
            out.write(readFully(input, size))
            readAsciiLine(input)
        }
        return out.toByteArray()
    }

    private fun readFully(input: InputStream, len: Int): ByteArray {
        val buf = ByteArray(len)
        var off = 0
        while (off < len) {
            val n = input.read(buf, off, len - off)
            if (n < 0) break
            off += n
        }
        return if (off == len) buf else buf.copyOf(off)
    }

    private fun readAsciiLine(input: InputStream): String {
        val out = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0 || b == '\n'.code) break
            if (b != '\r'.code) out.write(b)
        }
        return out.toString(StandardCharsets.US_ASCII.name())
    }

    private fun charsetFromContentType(contentType: String?): Charset {
        val raw = contentType ?: return StandardCharsets.UTF_8
        val charsetToken = raw.split(';').map { it.trim() }
            .firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?: return StandardCharsets.UTF_8
        return runCatching {
            Charset.forName(charsetToken.substringAfter('=').trim().trim('"'))
        }.getOrDefault(StandardCharsets.UTF_8)
    }

    private fun buildDnsQuery(host: String, id: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(id ushr 8 and 0xff)
        out.write(id and 0xff)
        out.write(0x01)
        out.write(0x00)
        out.write(0x00)
        out.write(0x01)
        repeat(6) { out.write(0x00) }
        host.split('.').forEach { label ->
            val bytes = label.toByteArray(StandardCharsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)
        out.write(0x00)
        out.write(0x01)
        out.write(0x00)
        out.write(0x01)
        return out.toByteArray()
    }

    private fun parseDnsARecords(data: ByteArray, length: Int): List<String> {
        if (length < 12) return emptyList()
        val qd = u16(data, 4)
        val an = u16(data, 6)
        var offset = 12
        repeat(qd) {
            offset = skipDnsName(data, offset, length) + 4
        }
        val ips = mutableListOf<String>()
        repeat(an) {
            if (offset + 10 > length) return ips
            offset = skipDnsName(data, offset, length)
            if (offset + 10 > length) return ips
            val type = u16(data, offset)
            val rdLength = u16(data, offset + 8)
            offset += 10
            if (offset + rdLength > length) return ips
            if (type == 1 && rdLength == 4) {
                ips += listOf(
                    data[offset].toInt() and 0xff,
                    data[offset + 1].toInt() and 0xff,
                    data[offset + 2].toInt() and 0xff,
                    data[offset + 3].toInt() and 0xff,
                ).joinToString(".")
            }
            offset += rdLength
        }
        return ips
    }

    private fun skipDnsName(data: ByteArray, start: Int, length: Int): Int {
        var i = start
        while (i < length) {
            val len = data[i].toInt() and 0xff
            if (len == 0) return i + 1
            if (len and 0xC0 == 0xC0) return i + 2
            i += 1 + len
        }
        return length
    }

    private fun u16(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 10_000

        /** 单次 UDP DNS 超时短一点，方便快速换下一个服务器 */
        const val UDP_DNS_TIMEOUT_MS = 3_000
        val DEFAULT_DNS_SERVERS = listOf(
            "223.5.5.5",   // 阿里 DNS
            "119.29.29.29", // DNSPod
            "114.114.114.114",
            "8.8.8.8",     // Google（国内常超时，放最后兜底）
        )
    }
}
