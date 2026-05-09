package com.origincoding.aquarius.shared.web.error

import com.origincoding.aquarius.shared.error.ResultCode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ResultCodeHttpStatusRegistry(
    mappers: List<ResultCodeHttpStatusMapper>
) {
    private val logger = KotlinLogging.logger { }

    private val statusByCode: Map<String, HttpStatus> = buildMap {
        val entries = mappers.flatMap { mapper ->
            mapper.mappings().map { (resultCode, status) ->
                MappingEntry(
                    code = resultCode.code,
                    status = status,
                    source = mapper::class.qualifiedName ?: mapper::class.simpleName.orEmpty()
                )
            }
        }

        val duplicates = entries.groupBy { it.code }.filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            "Duplicate ResultCode HTTP Status mappings: " +
                duplicates.entries.joinToString { (code, duplicatedEntries) ->
                    "$code -> ${duplicatedEntries.joinToString { "${it.source}: ${it.status}" }}"
                }
        }

        putAll(entries.map { it.code to it.status })
    }

    fun getStatus(resultCode: ResultCode): HttpStatus =
        statusByCode[resultCode.code] ?: HttpStatus.INTERNAL_SERVER_ERROR.also {
            logger.warn { "ResultCode [${resultCode.code}] has no HTTP status mapping, fallback to 500" }
        }

    private data class MappingEntry(
        val code: String,
        val status: HttpStatus,
        val source: String
    )
}
