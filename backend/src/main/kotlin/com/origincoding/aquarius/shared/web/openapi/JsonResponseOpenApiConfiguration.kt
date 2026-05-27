package com.origincoding.aquarius.shared.web.openapi

import com.origincoding.aquarius.shared.web.response.JsonResponse
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ResolvableType
import org.springframework.http.ResponseEntity
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import kotlin.reflect.full.primaryConstructor

@Configuration(proxyBeanMethods = false)
class JsonResponseOpenApiConfiguration(
    private val handlerMappings: List<RequestMappingHandlerMapping>,
) {
    @Bean
    fun jsonResponseOpenApiCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        val components = openApi.components ?: Components().also(openApi::components)
        val schemas = components.schemas ?: linkedMapOf<String, Schema<Any>>().also(components::schemas)
        val responseSchemas = jsonResponseSchemas()
        val responseSchemaByOperationId = responseSchemas.associateBy { it.operationId }

        if (responseSchemas.isEmpty()) {
            return@OpenApiCustomizer
        }

        schemas[JSON_RESPONSE_BASE_SCHEMA] = jsonResponseBaseSchema()

        responseSchemas.forEach { responseSchema ->
            schemas[responseSchema.name] = if (responseSchema.payloadType.isUnitLike()) {
                jsonResponseWithoutDataSchema()
            } else {
                val payloadSchemaName = registerPayloadSchema(responseSchema.payloadType, components)
                jsonResponseWithDataSchema(payloadSchemaName)
            }
        }

        openApi.paths?.values
            ?.flatMap { it.readOperations() }
            ?.forEach { operation ->
                val responseSchema = responseSchemaByOperationId[operation.operationId] ?: return@forEach
                operation.responses?.values
                    ?.flatMap { it.content?.values.orEmpty() }
                    ?.forEach { mediaType ->
                        mediaType.schema = schemaRef(responseSchema.name)
                    }
            }

        schemas.remove("WithData")
        schemas.remove("WithoutData")
    }

    private fun jsonResponseSchemas(): Set<JsonResponseSchema> =
        handlerMappings
            .flatMap { it.handlerMethods.values }
            .mapNotNull(::jsonResponseSchema)
            .toSet()

    private fun jsonResponseSchema(handlerMethod: HandlerMethod): JsonResponseSchema? {
        val responseType = jsonResponseType(
            ResolvableType.forMethodReturnType(handlerMethod.method, handlerMethod.beanType)
        ) ?: return null
        val payloadType = responseType.getGeneric(0).resolve() ?: return null

        return JsonResponseSchema(
            operationId = handlerMethod.method.name,
            name = "JsonResponse${payloadType.simpleName}",
            payloadType = payloadType,
        )
    }

    private fun jsonResponseType(type: ResolvableType): ResolvableType? {
        val rawType = type.resolve() ?: return null

        if (JsonResponse::class.java.isAssignableFrom(rawType)) {
            return type
        }

        if (ResponseEntity::class.java.isAssignableFrom(rawType)) {
            return jsonResponseType(type.getGeneric(0))
        }

        return null
    }

    private fun registerPayloadSchema(payloadType: Class<*>, components: Components): String {
        val resolvedSchema = ModelConverters.getInstance()
            .resolveAsResolvedSchema(AnnotatedType(payloadType))
        val schemaName = resolvedSchema.schema?.name ?: payloadType.simpleName
        val schemas = components.schemas ?: linkedMapOf<String, Schema<Any>>().also(components::schemas)

        resolvedSchema.referencedSchemas?.forEach { (name, schema) ->
            schemas.putIfAbsent(name, schema)
        }

        resolvedSchema.schema?.let { resolvedPayloadSchema ->
            val schema = resolvedPayloadSchema.asObjectSchemaIfNeeded()
            schema.applyKotlinObjectMetadata(payloadType)
            schemas[schemaName] = schema
        }

        return schemaName
    }

    @Suppress("UNCHECKED_CAST")
    private fun Schema<Any>.asObjectSchemaIfNeeded(): Schema<Any> {
        if (properties.isNullOrEmpty()) {
            return this
        }

        return ObjectSchema()
            .properties(properties)
            .description(description) as Schema<Any>
    }

    private fun Schema<*>.applyKotlinObjectMetadata(type: Class<*>) {
        if (!properties.isNullOrEmpty() && this.type == null) {
            this.type = "object"
            this.types = setOf("object")
        }

        val requiredProperties = type.kotlin.primaryConstructor
            ?.parameters
            ?.filter { parameter -> !parameter.isOptional && !parameter.type.isMarkedNullable }
            ?.mapNotNull { it.name }
            .orEmpty()

        if (requiredProperties.isNotEmpty()) {
            required(requiredProperties)
        }
    }

    private fun jsonResponseWithDataSchema(payloadSchemaName: String): Schema<Any> =
        ComposedSchema()
            .addAllOfItem(schemaRef(JSON_RESPONSE_BASE_SCHEMA))
            .addAllOfItem(
                ObjectSchema()
                    .required(listOf("data"))
                    .addProperty("data", schemaRef(payloadSchemaName))
            )

    private fun jsonResponseWithoutDataSchema(): Schema<Any> =
        ComposedSchema()
            .addAllOfItem(schemaRef(JSON_RESPONSE_BASE_SCHEMA))

    private fun jsonResponseBaseSchema(): Schema<Any> =
        ObjectSchema()
            .required(listOf("code", "timestamp", "requestId"))
            .addProperty("code", StringSchema())
            .addProperty("arguments", arrayRef("MessageArgument"))
            .addProperty("issues", arrayRef("IssueBody"))
            .addProperty("warnings", arrayRef("IssueBody"))
            .addProperty("timestamp", DateTimeSchema())
            .addProperty("requestId", StringSchema())

    private fun arrayRef(schemaName: String): ArraySchema =
        ArraySchema().items(schemaRef(schemaName))

    private fun schemaRef(schemaName: String): Schema<Any> =
        Schema<Any>().`$ref`("#/components/schemas/$schemaName")

    private fun Class<*>.isUnitLike(): Boolean =
        this == Unit::class.java || this == Void.TYPE || this == Void::class.java

    private data class JsonResponseSchema(
        val operationId: String,
        val name: String,
        val payloadType: Class<*>,
    )

    private companion object {
        const val JSON_RESPONSE_BASE_SCHEMA = "JsonResponseBase"
    }
}
