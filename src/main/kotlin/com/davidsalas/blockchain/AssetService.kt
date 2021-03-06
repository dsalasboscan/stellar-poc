package com.davidsalas.blockchain

import io.swagger.annotations.ApiOperation
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.stellar.sdk.AssetTypeCreditAlphaNum12
import org.stellar.sdk.Server
import org.stellar.sdk.responses.AssetResponse
import org.stellar.sdk.responses.Response
import java.util.*

@Service
class AssetService(val server: Server, val accountService: AccountService, val paymentService: PaymentService) {

    fun getAssetByEmail(@PathVariable("email") email: String): ArrayList<AssetResponse>? {
        val account = accountService.findByEmail(email)

        if (!account.issuingAccount) throw AssetException(message = "There is not any asset associated with: $email")
        return server.assets().assetIssuer(account.accountId).execute().records
    }

    fun create(request: CreateAssetRequest): Response {
        val sourceAccount = accountService.findByEmail(request.email)

        if (sourceAccount.issuingAccount) {
            val asset = AssetTypeCreditAlphaNum12(request.assetCode, sourceAccount.accountId)

            if (sourceAccount.distributionAccount == null) {
                throw AccountException(
                        "no_existent_distribution_account",
                        "To issue an Asset you need another account to send the original transaction, that account will act as distributor"
                )
            }

            return paymentService.sendAssetCreationPayment(sourceAccount, asset, request.limit)
        }

        throw AccountException("not_issuing_account", "the account must be a issuing account")
    }
}

@RestController
@RequestMapping("/asset")
class AssetController(val assetService: AssetService, val authorizationService: AuthorizationService) {

    @GetMapping("/{email}")
    @ApiOperation(value = "Get assets that belongs to the email")
    fun getAssetByEmail(@PathVariable("email") email: String) = assetService.getAssetByEmail(email)

    @PostMapping("/create")
    @ApiOperation(value = "Create an Asset and associate with an account")
    fun create(@RequestBody request: CreateAssetRequest) = assetService.create(request)

    @PostMapping("/authorize-external")
    @ApiOperation(value = "Authorize external Stellar user to use an Asset created by an account of this application")
    fun authorize(@RequestBody request: AuthorizeAssetRequest): Response {
        val asset = AssetTypeCreditAlphaNum12.createNonNativeAsset(request.assetData.assetCode, request.assetData.issuerAccountId)
        return authorizationService.authorize(request.secretSeed, asset, request.limit)
    }
}

data class CreateAssetRequest(val email: String, val assetCode: String, val limit: String)
data class AuthorizeAssetRequest(val secretSeed: String, val assetData: AssetData, val limit: String)
data class AssetData(val issuerAccountId: String, val assetCode: String)
