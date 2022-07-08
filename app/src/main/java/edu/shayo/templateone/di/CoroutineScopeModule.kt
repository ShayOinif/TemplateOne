package edu.shayo.templateone.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.shayo.templateone.service.LocationServiceCoroutineScope
import edu.shayo.templateone.service.LocationServiceCoroutineScopeImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class CoroutineScopeModule {
    @Singleton
    @Binds
    abstract fun bindLocationServiceCoroutineScope(impl: LocationServiceCoroutineScopeImpl): LocationServiceCoroutineScope
}

