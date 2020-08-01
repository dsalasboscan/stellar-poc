package com.davidsalas.blockchain

import org.springframework.stereotype.Service
import org.stellar.sdk.*
import org.stellar.sdk.responses.Response

@Service
class AuthorizationService(val server: Server) {

    fun authorize(destinationSecretSeed: String, asset: Asset, limit: String): Response {
        val receivingKeyPair = KeyPair.fromSecretSeed(destinationSecretSeed)
        val account = server.accounts().account(receivingKeyPair.accountId)
        val transaction = Transaction.Builder(account, Network.TESTNET)
                .addOperation(
                        ChangeTrustOperation.Builder(asset, limit).build()
                )
                .setTimeout(10000)
                .setBaseFee(100)
                .build()

        transaction.sign(receivingKeyPair)
        return server.submitTransaction(transaction)
    }
}