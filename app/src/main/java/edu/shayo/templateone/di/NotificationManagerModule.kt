package edu.shayo.templateone.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.shayo.templateone.notifications.NotificationManager
import edu.shayo.templateone.notifications.NotificationManagerImpl

@InstallIn(SingletonComponent::class)
@Module
abstract class NotificationManagerModule {

    @Binds
    abstract fun bindNotificationManager(impl: NotificationManagerImpl): NotificationManager
}