package com.lurenjia534.nextonedrivev3.di

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lurenjia534.nextonedrivev2.AuthRepository.AuthenticationCallbackProvider
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthenticationManager
import com.lurenjia534.nextonedrivev3.AuthRepository.TokenManager
import com.lurenjia534.nextonedrivev3.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.json.JSONObject
import javax.inject.Singleton
import com.lurenjia534.nextonedrivev3.AuthRepository.AuthViewModel

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
    }
    
    @Provides
    fun provideClientId(@ApplicationContext context: Context): String? {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.msal_config)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString("client_id")
        } catch (e: Exception) {
            null
        }
    }
    
    @Provides
    @Singleton
    fun provideAccessTokenState(): MutableState<String?> = mutableStateOf(null)
    
    @Provides
    @Singleton
    fun provideMsalInitializedState(): MutableState<Boolean> = mutableStateOf(false)
    
    @Provides
    @Singleton
    fun provideAuthenticationManager(
        @ApplicationContext context: Context,
        tokenManager: TokenManager,
        clientId: String?,
        accessTokenState: MutableState<String?>,
        isMsalInitializedState: MutableState<Boolean>
    ): AuthenticationManager {
        return AuthenticationManager(
            context = context,
            tokenManager = tokenManager,
            clientId = clientId,
            accessTokenState = accessTokenState,
            isMsalInitializedState = isMsalInitializedState
        )
    }

    @Provides
    @Singleton
    fun provideAuthViewModel(
        authenticationManager: AuthenticationManager,
        accessTokenState: MutableState<String?>,
        isMsalInitializedState: MutableState<Boolean>,
        tokenManager: TokenManager
    ): AuthViewModel {
        return AuthViewModel(
            authenticationManager = authenticationManager,
            accessTokenState = accessTokenState,
            isMsalInitializedState = isMsalInitializedState,
            tokenManager = tokenManager
        )
    }
} 