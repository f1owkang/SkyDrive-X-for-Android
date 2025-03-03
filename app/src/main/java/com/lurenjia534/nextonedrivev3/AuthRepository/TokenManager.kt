// 文件 com.lurenjia534.nextonedrivev2.AuthRepository

package com.lurenjia534.nextonedrivev3.AuthRepository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MSAL_PREFS", Context.MODE_PRIVATE)

    fun saveAccessToken(token: String?) {
        sharedPreferences.edit().putString("ACCESS_TOKEN", token).apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString("ACCESS_TOKEN", null)
    }

    fun saveAccountId(accountId: String?) {
        sharedPreferences.edit().putString("ACCOUNT_ID", accountId).apply()
    }

    fun getAccountId(): String? {
        return sharedPreferences.getString("ACCOUNT_ID", null)
    }

    fun saveAccountName(name: String?) {
        sharedPreferences.edit().putString("ACCOUNT_NAME", name).apply()
    }

    fun getAccountName(): String? {
        return sharedPreferences.getString("ACCOUNT_NAME", "我的OneDrive")
    }

    fun saveMultipleAccounts(accounts: List<AccountInfo>) {
        val accountsJson = accounts.joinToString("|") { "${it.id}::${it.name}::${it.token}" }
        sharedPreferences.edit().putString("MULTIPLE_ACCOUNTS", accountsJson).apply()
    }

    fun getMultipleAccounts(): List<AccountInfo> {
        val accountsJson = sharedPreferences.getString("MULTIPLE_ACCOUNTS", null) ?: return emptyList()
        if (accountsJson.isEmpty()) return emptyList()
        
        return accountsJson.split("|").mapNotNull { accountStr ->
            val parts = accountStr.split("::")
            if (parts.size >= 3) {
                AccountInfo(id = parts[0], name = parts[1], token = parts[2])
            } else null
        }
    }
}