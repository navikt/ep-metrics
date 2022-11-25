package no.nav.eessi.pensjon.metrics

import java.net.URI

internal fun simplifyUri(uri: URI): String {

    val sb = StringBuilder()
    if (uri.isOpaque) {
        sb.append(digitsReplace(uri.schemeSpecificPart))
    } else {
        if (uri.host != null) {
            sb.append(uri.host.replace("[", "").replace("]", "").split(":")[0].split(".")[0])
            sb.append(':')
            if (uri.port != -1) {
                sb.append(uri.port)
            }
        } else if (uri.authority != null) {
            sb.append(uri.authority)
        }
        if (uri.path != null) sb.append(digitsReplace(uri.path))
        if (uri.query != null) {
            sb.append('?')
            sb.append(valuesReplace(uri.query))
        }
    }
    return sb.toString()
}

internal fun simplifyUri(uri: String): String {
    return try {
        simplifyUri(URI(uri))
    } catch (e: Exception) {
        digitsReplace(uri)
    }
}

private fun valuesReplace(query: String) =
    query.split("&")
        .joinToString("&") {
            it.split("=").let { it[0] + "={}" }
        }

private fun digitsReplace(uri: String) = uri.replace(Regex("""\d{2,}"""), "{}")
