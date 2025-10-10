package com.cpen321.usermanagement.di

import android.content.Context
import com.cpen321.usermanagement.BuildConfig
import com.cpen321.usermanagement.data.local.preferences.TokenManager
import com.cpen321.usermanagement.network.SocketClient
import com.cpen321.usermanagement.ui.navigation.NavigationStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideTokenManager(
        @ApplicationContext context: Context
    ): TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideNavigationStateManager(): NavigationStateManager {
        return NavigationStateManager()
    }

    @Provides
    @Singleton
    fun provideSocketClient(): SocketClient {
        val client = SocketClient(BuildConfig.IMAGE_BASE_URL)
        // start a small debug logger so socket events are visible in logcat during development
        SocketDebugLogger(client)
        return client
    }
}
