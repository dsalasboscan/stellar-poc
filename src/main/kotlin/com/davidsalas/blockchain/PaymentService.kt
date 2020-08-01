package com.davidsalas.blockchain

import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.stellar.sdk.*
import org.stellar.sdk.responses.Response
import org.stellar.sdk.responses.SubmitTransactionResponse
import org.stellar.sdk.responses.SubmitTransactionTimeoutResponseException

@Service
class PaymentService(
        val server: Server,
        val accountService: AccountService,
        val authorizationService: AuthorizationService,
        val errorHandlerService: ErrorHandlerService,
        val retryTemplate: RetryTemplate
) {
    companion object {
        const val DEFAULT_TRANSACTION_TEXT = "TX Test"
        const val DEFAULT_LIMIT = "1000000"
    }

    fun sendPayment(request: PaymentRequest): Response {
        val sourceAccount = accountService.findByEmail(request.emailSource)

        val asset =
                if (request.assetData != null) AssetTypeCreditAlphaNum12(request.assetData.assetCode, request.assetData.issuerAccountId)
                else AssetTypeNative()

        return when (request.accountType) {
            AccountType.INTERNAL -> sendInternalUser(sourceAccount, request.emailDestination, asset, request.amount)
            AccountType.EXTERNAL -> sendExternalUser(sourceAccount, request.destinationAccountId, asset, request.amount)
        }
    }

    fun sendAssetCreationPayment(sourceAccount: Account, asset: Asset, limit: String): Response {
        authorizationService.authorize(sourceAccount.distributionAccount!!.secretSeed, asset, limit)
        val paymentTransaction = PaymentOperationData(sourceAccount.secretSeed, sourceAccount.distributionAccount!!.accountId, asset, limit)
        return errorHandlerService.handle { doSend(paymentTransaction) }
    }

    private fun sendInternalUser(sourceAccount: Account, emailDestination: String?, asset: Asset, amount: String): Response {
        val destinationAccount = if (emailDestination != null) {
            accountService.findByEmail(emailDestination)
        } else throw AccountException("Destination internal user with email: $emailDestination don't exist}")

        val paymentTransaction = PaymentOperationData(sourceAccount.secretSeed, destinationAccount.accountId, asset, amount)

        return try {
            errorHandlerService.handle { doSend(paymentTransaction) }
        } catch (e: HorizonException) {
            authorizationService.authorize(destinationAccount.secretSeed, asset, DEFAULT_LIMIT)
            doSend(paymentTransaction)
        }
    }

    private fun sendExternalUser(sourceAccount: Account, destinationAccountId: String?, asset: Asset, amount: String): Response {
        if (destinationAccountId == null) throw PaymentException("Destination account id must not be null for external users")

        val paymentTransaction = PaymentOperationData(sourceAccount.secretSeed, destinationAccountId, asset, amount)

        return errorHandlerService.handle { doSend(paymentTransaction) }
    }

    private fun doSend(paymentOperationData: PaymentOperationData): SubmitTransactionResponse {
        val sourceKeyPair = KeyPair.fromSecretSeed(paymentOperationData.sourceAccountSeed)
        val sourceAccount = server.accounts().account(sourceKeyPair.accountId)
        val paymentOperation = PaymentOperation.Builder(
                paymentOperationData.destinationAccountId, paymentOperationData.asset, paymentOperationData.amount).build()

        val transaction = Transaction.Builder(sourceAccount, Network.TESTNET)
                .addOperation(paymentOperation)
                .addMemo(Memo.text(DEFAULT_TRANSACTION_TEXT))
                .setTimeout(10000)
                .setBaseFee(100)
                .build()

        transaction.sign(sourceKeyPair)

        return retryTemplate.execute<SubmitTransactionResponse, SubmitTransactionTimeoutResponseException> {
            server.submitTransaction(transaction)
        }
    }
}

@RestController
class PaymentController(val paymentService: PaymentService) {

    @PostMapping("/payment/send-asset")
    fun sendPayment(@RequestBody request: PaymentRequest) = paymentService.sendPayment(request)
}

data class PaymentRequest(val accountType: AccountType, val emailSource: String, val emailDestination: String? = null, val destinationAccountId: String? = null, val assetData: AssetData? = null, val amount: String)
data class PaymentOperationData(val sourceAccountSeed: String, val destinationAccountId: String, val asset: Asset, val amount: String)