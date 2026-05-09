package com.origincoding.aquarius.shared.web.error

import com.origincoding.aquarius.shared.error.BusinessException
import com.origincoding.aquarius.shared.error.GlobalResultCode
import com.origincoding.aquarius.shared.error.IssueBody
import com.origincoding.aquarius.shared.error.MessageArgument
import com.origincoding.aquarius.shared.error.ResultCode
import com.origincoding.aquarius.shared.error.ValidationIssueCode
import com.origincoding.aquarius.shared.web.response.JsonResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Negative
import jakarta.validation.constraints.NegativeOrZero
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.beans.TypeMismatchException
import org.springframework.context.MessageSourceResolvable
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.validation.method.ParameterErrors
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import kotlin.reflect.KClass

@RestControllerAdvice
class GlobalControllerAdvice(
    private val resultCodeHttpStatusRegistry: ResultCodeHttpStatusRegistry
) {
    private val logger = KotlinLogging.logger { }

    @ExceptionHandler(BusinessException::class)
    fun handle(e: BusinessException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn(e) { "Business exception: code=${e.resultCode.code}" }
        return error(e.resultCode, e.arguments, e.issues)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(e: MethodArgumentNotValidException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Request validation failed: object=${e.bindingResult.objectName}" }
        return error(GlobalResultCode.REQUEST_VALIDATION_FAILED, issues = validationIssues(e.bindingResult))
    }

    @ExceptionHandler(BindException::class)
    fun handle(e: BindException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Request binding validation failed: object=${e.bindingResult.objectName}" }
        return error(GlobalResultCode.REQUEST_VALIDATION_FAILED, issues = validationIssues(e.bindingResult))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(e: ConstraintViolationException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Constraint validation failed: count=${e.constraintViolations.size}" }
        return error(
            GlobalResultCode.REQUEST_VALIDATION_FAILED,
            issues = e.constraintViolations.map(::constraintViolationIssue)
        )
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handle(e: HandlerMethodValidationException): ResponseEntity<JsonResponse<Unit>> {
        if (e.isForReturnValue) {
            logger.error(e) { "Handler method return value validation failed" }
            return error(GlobalResultCode.SYSTEM_INTERNAL)
        }

        logger.warn { "Handler method validation failed: count=${e.parameterValidationResults.size}" }
        return error(
            GlobalResultCode.REQUEST_VALIDATION_FAILED,
            issues = methodValidationIssues(e)
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handle(e: HttpMessageNotReadableException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn(e) { "Request body is malformed" }
        return error(GlobalResultCode.REQUEST_MALFORMED)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handle(e: MethodArgumentTypeMismatchException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Request argument type mismatch: parameter=${e.name}, value=${e.value}" }
        return error(
            GlobalResultCode.REQUEST_MALFORMED,
            issues = listOf(
                IssueBody(
                    code = "request.parameter.type_mismatch",
                    field = e.name
                )
            )
        )
    }

    @ExceptionHandler(TypeMismatchException::class)
    fun handle(e: TypeMismatchException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Request type mismatch: property=${e.propertyName}, value=${e.value}" }
        return error(
            GlobalResultCode.REQUEST_MALFORMED,
            issues = listOfNotNull(
                e.propertyName?.let { IssueBody(code = "request.parameter.type_mismatch", field = it) }
            )
        )
    }

    @ExceptionHandler(ServletRequestBindingException::class)
    fun handle(e: ServletRequestBindingException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Request binding is malformed: ${e.message}" }
        return error(GlobalResultCode.REQUEST_MALFORMED)
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handle(e: HttpRequestMethodNotSupportedException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "HTTP method is not supported: method=${e.method}" }
        return error(
            GlobalResultCode.REQUEST_METHOD_NOT_ALLOWED,
            arguments = listOf(MessageArgument("method", e.method))
        )
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handle(e: HttpMediaTypeNotSupportedException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "HTTP media type is not supported: contentType=${e.contentType}" }
        return error(
            GlobalResultCode.REQUEST_UNSUPPORTED_MEDIA_TYPE,
            arguments = e.contentType?.let { listOf(MessageArgument("contentType", it.toString())) }.orEmpty()
        )
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handle(e: MaxUploadSizeExceededException): ResponseEntity<JsonResponse<Unit>> {
        logger.warn(e) { "Request payload is too large" }
        return error(GlobalResultCode.REQUEST_PAYLOAD_TOO_LARGE)
    }

    @ExceptionHandler(NoHandlerFoundException::class, NoResourceFoundException::class)
    fun handleNotFound(e: Exception): ResponseEntity<JsonResponse<Unit>> {
        logger.warn { "Resource was not found: ${e.message}" }
        return error(GlobalResultCode.RESOURCE_NOT_FOUND)
    }

    @ExceptionHandler(DataIntegrityViolationException::class, ObjectOptimisticLockingFailureException::class)
    fun handleConflict(e: Exception): ResponseEntity<JsonResponse<Unit>> {
        logger.warn(e) { "Resource conflict" }
        return error(GlobalResultCode.RESOURCE_CONFLICT)
    }

    @ExceptionHandler(Exception::class)
    fun handle(e: Exception): ResponseEntity<JsonResponse<Unit>> {
        logger.error(e) { "Unhandled exception" }
        return error(GlobalResultCode.SYSTEM_INTERNAL)
    }

    private fun error(
        resultCode: ResultCode,
        arguments: List<MessageArgument> = emptyList(),
        issues: List<IssueBody> = emptyList()
    ): ResponseEntity<JsonResponse<Unit>> =
        ResponseEntity
            .status(resultCodeHttpStatusRegistry.getStatus(resultCode))
            .body(JsonResponse.error(resultCode, arguments, issues))

    private fun validationIssues(bindingResult: BindingResult): List<IssueBody> =
        validationIssues(bindingResult as Errors)

    private fun validationIssues(errors: Errors): List<IssueBody> =
        errors.fieldErrors.map(::fieldValidationIssue) +
            errors.globalErrors.map(::objectValidationIssue)

    private fun methodValidationIssues(e: HandlerMethodValidationException): List<IssueBody> =
        e.parameterValidationResults.flatMap(::parameterValidationIssues) +
            e.crossParameterValidationResults.map {
                IssueBody(code = validationCode(it.codes))
            }

    private fun parameterValidationIssues(result: ParameterValidationResult): List<IssueBody> {
        if (result is ParameterErrors) {
            return validationIssues(result)
        }

        val field = result.methodParameter.parameterName

        return result.resolvableErrors.map { error ->
            val violation = unwrapConstraintViolation(result, error)
            if (violation != null) {
                constraintViolationIssue(violation, field)
            } else {
                IssueBody(
                    code = validationCode(error.codes),
                    field = field
                )
            }
        }
    }

    private fun fieldValidationIssue(error: FieldError): IssueBody {
        val violation = unwrapConstraintViolation(error)

        return IssueBody(
            code = violation?.let { validationCode(it.constraintDescriptor.annotation.annotationClass) }
                ?: validationCode(error.codes),
            field = error.field,
            arguments = violation?.let(::constraintArguments).orEmpty()
        )
    }

    private fun objectValidationIssue(error: ObjectError): IssueBody {
        val violation = unwrapConstraintViolation(error)

        return IssueBody(
            code = violation?.let { validationCode(it.constraintDescriptor.annotation.annotationClass) }
                ?: validationCode(error.codes),
            arguments = violation?.let(::constraintArguments).orEmpty()
        )
    }

    private fun constraintViolationIssue(violation: ConstraintViolation<*>): IssueBody =
        constraintViolationIssue(violation, violation.propertyPath.lastOrNull()?.name)

    private fun constraintViolationIssue(violation: ConstraintViolation<*>, field: String?): IssueBody =
        IssueBody(
            code = validationCode(violation.constraintDescriptor.annotation.annotationClass),
            field = field,
            arguments = constraintArguments(violation)
        )

    private fun validationCode(annotationType: KClass<out Annotation>): String =
        when (annotationType) {
            NotNull::class, NotBlank::class, NotEmpty::class -> ValidationIssueCode.REQUIRED
            Email::class -> ValidationIssueCode.EMAIL_INVALID
            Size::class -> ValidationIssueCode.SIZE_INVALID
            Min::class -> ValidationIssueCode.MIN
            Max::class -> ValidationIssueCode.MAX
            DecimalMin::class -> ValidationIssueCode.DECIMAL_MIN
            DecimalMax::class -> ValidationIssueCode.DECIMAL_MAX
            Pattern::class -> ValidationIssueCode.PATTERN_INVALID
            Positive::class -> ValidationIssueCode.POSITIVE
            PositiveOrZero::class -> ValidationIssueCode.POSITIVE_OR_ZERO
            Negative::class -> ValidationIssueCode.NEGATIVE
            NegativeOrZero::class -> ValidationIssueCode.NEGATIVE_OR_ZERO
            else -> ValidationIssueCode.INVALID
        }

    private fun validationCode(sourceCodes: Array<String>?): String =
        sourceCodes
            ?.firstNotNullOfOrNull(::validationCodeOrNull)
            ?: ValidationIssueCode.INVALID

    private fun validationCodeOrNull(sourceCode: String): String? =
        when (sourceCode.substringBefore('.')) {
            "NotNull", "NotBlank", "NotEmpty" -> ValidationIssueCode.REQUIRED
            "Email" -> ValidationIssueCode.EMAIL_INVALID
            "Size", "Length" -> ValidationIssueCode.SIZE_INVALID
            "Min" -> ValidationIssueCode.MIN
            "Max" -> ValidationIssueCode.MAX
            "DecimalMin" -> ValidationIssueCode.DECIMAL_MIN
            "DecimalMax" -> ValidationIssueCode.DECIMAL_MAX
            "Pattern" -> ValidationIssueCode.PATTERN_INVALID
            "Positive" -> ValidationIssueCode.POSITIVE
            "PositiveOrZero" -> ValidationIssueCode.POSITIVE_OR_ZERO
            "Negative" -> ValidationIssueCode.NEGATIVE
            "NegativeOrZero" -> ValidationIssueCode.NEGATIVE_OR_ZERO
            else -> null
        }

    private fun constraintArguments(violation: ConstraintViolation<*>): List<MessageArgument> {
        val includedNames = setOf("min", "max", "value", "regexp", "integer", "fraction")

        return violation.constraintDescriptor.attributes
            .filterKeys { it in includedNames }
            .map { (name, value) -> MessageArgument(name, value.toString()) }
    }

    private fun unwrapConstraintViolation(error: ObjectError): ConstraintViolation<*>? =
        runCatching { error.unwrap(ConstraintViolation::class.java) }.getOrNull()

    private fun unwrapConstraintViolation(
        result: ParameterValidationResult,
        error: MessageSourceResolvable
    ): ConstraintViolation<*>? =
        runCatching { result.unwrap(error, ConstraintViolation::class.java) }.getOrNull()
}
