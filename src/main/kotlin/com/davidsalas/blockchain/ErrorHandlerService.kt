package com.davidsalas.blockchain

import com.davidsalas.blockchain.ErrorHandlerService.Companion.DEFAULT_CODE
import com.davidsalas.blockchain.ErrorHandlerService.Companion.DEFAULT_MESSAGE
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.stellar.sdk.responses.Response
import org.stellar.sdk.responses.SubmitTransactionResponse

@Service
class ErrorHandlerService {

    companion object {
        const val DEFAULT_CODE = "ERROR"
        const val DEFAULT_MESSAGE = "Unexpected error"
        const val NOT_TRUSTED_COIN_ERROR_CODE = "op_no_trust"
    }

    fun <T : Response> handle(process: () -> T): T {

        val response = process()

        when (response) {
            is SubmitTransactionResponse -> {
                if (response.extras != null && response.extras.resultCodes.operationsResultCodes.contains(NOT_TRUSTED_COIN_ERROR_CODE)) {
                    throw HorizonException(NOT_TRUSTED_COIN_ERROR_CODE, "You need to authorize the Asset to use it")
                }
            }
        }

        return response
    }
}

@RestControllerAdvice
class ControllerAdvice : ResponseEntityExceptionHandler() {

    @ExceptionHandler(Exception::class)
    fun handleBaseException(e: Exception): ErrorDto {
        return when (e) {
            is DataAccessException -> ErrorDto(HttpStatus.INTERNAL_SERVER_ERROR.name, e.mostSpecificCause.message ?: DEFAULT_MESSAGE)
            is BaseException -> ErrorDto(e.code, e.message)
            else -> ErrorDto(DEFAULT_CODE, e.message ?: DEFAULT_MESSAGE)
        }
    }
}

data class ErrorDto(val code: String, val message: String)

open class BaseException(val code: String, override val message: String) : RuntimeException()
class HorizonException(code: String = "horizon_api_error", message: String = "") : BaseException(code, message)
class AssetException(code: String = "asset_error", message: String) : BaseException(code, message)
class AccountException(code: String = "account_error", message: String) : BaseException(code, message)
class PaymentException(code: String = "payment_error", message: String) : BaseException(code, message)
