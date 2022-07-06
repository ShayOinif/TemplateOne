package edu.shayo.templateone.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.shayo.templateone.navigator.Navigator
import edu.shayo.templateone.navigator.NavigatorImpl
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class NavigatorModule {

    @Singleton
    @Binds
    abstract fun bindNavigator(impl: NavigatorImpl): Navigator
}