package com.cpen321.usermanagement.di

import com.cpen321.usermanagement.network.SocketClient
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Shared Hilt entry point to obtain the singleton SocketClient from non-Hilt-managed classes
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SocketClientEntryPoint {
    fun socketClient(): SocketClient
}
