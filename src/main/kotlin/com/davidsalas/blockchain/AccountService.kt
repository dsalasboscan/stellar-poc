package com.davidsalas.blockchain

import com.davidsalas.blockchain.common.Constants.STELLAR_BOT_URL
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Server
import org.stellar.sdk.responses.AccountResponse
import java.net.URL
import java.util.*
import javax.persistence.*

@Service
class AccountService(val server: Server, val accountRepository: AccountRepository) {

    fun findByEmail(email: String): Account = accountRepository.findByEmail(email)
            .orElseThrow { throw AccountException("Account with email: $email don't exist") }

    fun create(request: CreateAccountRequest): ResponseEntity<CreateAccountResponse> {
        if (request.issuingAccount && request.distributorEmail == null) {
            throw HorizonException("no_distributor_email", "Issuing accounts MUST have a distributor account with an email")
        }

        val accountKeyPair = KeyPair.random()
        getMoneyFromTestBot(accountKeyPair.accountId)
        server.accounts().account(accountKeyPair.accountId)

        val account = Account(
                secretSeed = String(accountKeyPair.secretSeed),
                accountId = accountKeyPair.accountId,
                email = request.email,
                issuingAccount = request.issuingAccount
        )

        var distributorAccountId: String? = null

        if (account.issuingAccount) {
            val distributorAccountKeyPair = KeyPair.random()
            distributorAccountId = distributorAccountKeyPair.accountId
            getMoneyFromTestBot(distributorAccountKeyPair.accountId)
            server.accounts().account(distributorAccountKeyPair.accountId)

            account.distributionAccount = Account(
                    secretSeed = String(distributorAccountKeyPair.secretSeed),
                    accountId = distributorAccountKeyPair.accountId,
                    email = request.distributorEmail!!,
                    issuingAccount = false
            )
        }

        accountRepository.save(account)

        val response = CreateAccountResponse(accountId = account.accountId, distributorAccountId = distributorAccountId)

        return ResponseEntity(response, HttpStatus.OK)
    }

    fun getBalance(email: String): Array<out AccountResponse.Balance> {
        val storedAccount = this.findByEmail(email)

        val account =
                if (storedAccount.issuingAccount) server.accounts().account(storedAccount.distributionAccount!!.accountId)
                else server.accounts().account(storedAccount.accountId)
        return account.balances
    }

    private fun getMoneyFromTestBot(accountId: String) = URL("$STELLAR_BOT_URL?addr=${accountId}").openStream()
}

@RestController
@RequestMapping("/account")
class AccountController(val accountService: AccountService) {

    @PostMapping("/create")
    fun create(@RequestBody request: CreateAccountRequest) = accountService.create(request)

    @GetMapping("/{email}/balance")
    fun getBalance(@PathVariable("email") email: String) = accountService.getBalance(email)
}

@Repository
interface AccountRepository : JpaRepository<Account, Long> {
    fun findByEmail(email: String): Optional<Account>
}

@Entity
data class Account(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @Column(unique = true) val email: String,
        @Column(unique = true) val secretSeed: String,
        @Column(unique = true) val accountId: String,
        val issuingAccount: Boolean,
        @OneToOne(cascade = [CascadeType.ALL]) var distributionAccount: Account? = null
)

data class CreateAccountRequest(val email: String, val distributorEmail: String? = null, val issuingAccount: Boolean = true)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateAccountResponse(val message: String = "account created", val accountId: String, val distributorAccountId: String? = null)

enum class AccountType { INTERNAL, EXTERNAL }