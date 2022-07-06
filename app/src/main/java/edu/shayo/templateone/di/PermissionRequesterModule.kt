package edu.shayo.templateone.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.shayo.templateone.permissions.PermissionRequester
import edu.shayo.templateone.permissions.PermissionRequesterImpl

@InstallIn(SingletonComponent::class)
@Module
abstract class PermissionRequesterModule {

    @Binds
    abstract fun bindPermissionRequester(impl: PermissionRequesterImpl): PermissionRequester
}