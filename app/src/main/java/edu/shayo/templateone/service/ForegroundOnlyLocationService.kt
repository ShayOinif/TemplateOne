package edu.shayo.templateone.service

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import edu.shayo.templateone.MainActivity
import edu.shayo.templateone.R
import edu.shayo.templateone.notifications.NotificationManager
import edu.shayo.templateone.utils.SharedPreferenceUtil
import edu.shayo.templateone.utils.locationFlow
import edu.shayo.templateone.utils.toText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.*

private const val PACKAGE_NAME = "edu.shayo.templateone"

private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
    "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

class ForegroundOnlyLocationService : LifecycleService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ForegroundOnlyLocationServiceEntryPoint {
        fun notificationManager(): NotificationManager

        fun locationServiceCoroutineScope(): LocationServiceCoroutineScope
    }

    private var canRunInBackground = false

    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val notificationChannelId = "$PACKAGE_NAME.${UUID.randomUUID()}"

    private val notificationId = (10000000..12345678).random()

    private lateinit var notificationManager: NotificationManager

    private lateinit var locationServiceCoroutineScope: LocationServiceCoroutineScope

    private var locationJob: Job? = null

    override fun onCreate() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        notificationManager = EntryPointAccessors.fromApplication(
            applicationContext,
            ForegroundOnlyLocationServiceEntryPoint::class.java
        ).notificationManager()

        locationServiceCoroutineScope = EntryPointAccessors.fromApplication(
            applicationContext,
            ForegroundOnlyLocationServiceEntryPoint::class.java
        ).locationServiceCoroutineScope()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cancelLocationTrackingFromNotification =
            intent?.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)
                ?: false

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (!configurationChange &&
            SharedPreferenceUtil.getLocationTrackingPref(this) &&
            canRunInBackground
        ) {
            val notification =
                notificationManager.generateNotification(
                    privateLocationFlow.replayCache.last().toText(),
                    getString(R.string.app_name),
                    getListOfAction(),
                    notificationChannelId,
                )

            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )

            serviceRunningInForeground = true
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    private fun subscribeToLocationUpdates(includingBackground: Boolean) {

        canRunInBackground = includingBackground

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        try {
            if (locationJob == null) {
                locationJob = lifecycleScope.launch(Dispatchers.IO) {

                    fusedLocationProviderClient.locationFlow(
                        5_000,
                        10_000,
                        20_000,
                        Priority.PRIORITY_HIGH_ACCURACY
                    ).collect { lastLocation ->
                        if (serviceRunningInForeground) {
                            notificationManager.notify(
                                notificationId,
                                notificationManager.generateNotification(
                                    lastLocation.toText(),
                                    getString(R.string.app_name),
                                    getListOfAction(),
                                    notificationChannelId
                                )
                            )
                        }

                        locationServiceCoroutineScope.launch {
                            privateLocationFlow.emit(lastLocation)
                        }
                    }
                }
            }
        } catch (unlikely: SecurityException) {
            Log.d("Shay", unlikely.message.toString())
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        }
    }

    private fun unsubscribeToLocationUpdates() {
        try {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            locationJob?.cancel()
            locationJob = null
            stopSelf()

        } catch (unlikely: SecurityException) {
            Log.d("Shay", unlikely.message.toString())
            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
        }
    }

    inner class LocalBinder : Binder() {
        fun subscribeToLocationUpdates(includingBackground: Boolean = false) =
            this@ForegroundOnlyLocationService.subscribeToLocationUpdates(includingBackground)

        fun unsubscribeToLocationUpdates() =
            this@ForegroundOnlyLocationService.unsubscribeToLocationUpdates()
    }

    private fun getListOfAction(): List<NotificationCompat.Action> {
        val launchActivityIntent = Intent(this, MainActivity::class.java)

        val cancelIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

        val servicePendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val activityAction = NotificationCompat.Action(
            R.drawable.ic_launch,
            getString(R.string.launch_activity),
            activityPendingIntent
        )

        val serviceAction = NotificationCompat.Action(
            R.drawable.ic_cancel,
            getString(R.string.stop_location_updates_button_text),
            servicePendingIntent
        )


        return listOf(
            activityAction,
            serviceAction
        )
    }

    companion object {
        private var privateLocationFlow = MutableSharedFlow<Location?>(1)
        val locationFlow: SharedFlow<Location?>
            get() = privateLocationFlow
    }
}