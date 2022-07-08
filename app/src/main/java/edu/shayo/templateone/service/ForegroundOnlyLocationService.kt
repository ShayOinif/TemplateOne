package edu.shayo.templateone.service

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import edu.shayo.templateone.MainActivity
import edu.shayo.templateone.R
import edu.shayo.templateone.notifications.NotificationManager
import edu.shayo.templateone.utils.SharedPreferenceUtil
import edu.shayo.templateone.utils.toText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

private const val PACKAGE_NAME = "edu.shayo.templateone"
private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
    "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

class ForegroundOnlyLocationService : LifecycleService() {

    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val notificationChannelId = "$PACKAGE_NAME.${UUID.randomUUID()}"

    private val notificationId = (10000000..12345678).random()

    private var locationFlow: SharedFlow<Location?>? = null

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ForegroundOnlyLocationServiceEntryPoint {
        fun notificationManager(): NotificationManager
    }

    private lateinit var notificationManager: NotificationManager

    private var currentScope: CoroutineScope? = null

    override fun onCreate() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        notificationManager = EntryPointAccessors.fromApplication(
            applicationContext,
            ForegroundOnlyLocationServiceEntryPoint::class.java
        ).notificationManager()
    }

    // Implementation of a cold flow backed by a Channel that sends Location updates
    @OptIn(FlowPreview::class)
    private fun FusedLocationProviderClient.locationFlow() = callbackFlow {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                try {
                    Log.d("Shay", "In callback")
                    trySend(result.lastLocation)
                } catch (e: Exception) {
                    Log.d("Shay", e.message.toString())
                }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 1_000
            fastestInterval = 1_000
            maxWaitTime = 1_000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            .addOnFailureListener { e ->
                close(e) // in case of exception, close the Flow
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    locationFlow = null
                }
            }
        // clean up when Flow collection ends

        awaitClose {
            removeLocationUpdates(locationCallback)
        }
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
        if (!configurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {

            val notification =
                notificationManager.generateNotification(
                   /* locationFlow?.
                    .replayCache?.last().toText(),*/
                    "test",
                    getString(R.string.app_name),
                    getListOfAction(),
                    notificationChannelId,
                )

            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
            serviceRunningInForeground = true
        }

        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    private fun subscribeToLocationUpdates() {

        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        startService(Intent(applicationContext, ForegroundOnlyLocationService::class.java))

        val test = CoroutineScope(Dispatchers.IO + SupervisorJob())

        locationFlow = fusedLocationProviderClient.locationFlow().shareIn(test, SharingStarted.Eagerly, 1)

        try {

            lifecycleScope.launch(Dispatchers.IO) {
                launch {
                        //.shareIn(currentScope!!, SharingStarted.Eagerly, 1)
                        //locationFlow?.collect { lastLocation ->

                    locationFlow?.collectLatest {lastLocation ->
                        Log.d("Shay", lastLocation.toText())

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
                    }
                }

                launch {
                    while (true) {
                        Log.d("Shay", lifecycle.currentState.name)
                        locationFlow?.let {
                            Log.d("Shay", "Location flow not null")
                        }

                        delay(3000)
                    }
                }
            }
            // }

        } catch (unlikely: SecurityException) {
            Log.d("Shay", unlikely.message.toString())
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        }
    }

    private fun unsubscribeToLocationUpdates() {
        try {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            stopSelf()

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
        }
    }

    inner class LocalBinder : Binder() {
        fun subscribeToLocationUpdates() =
            this@ForegroundOnlyLocationService.subscribeToLocationUpdates()

        fun unsubscribeToLocationUpdates() =
            this@ForegroundOnlyLocationService.unsubscribeToLocationUpdates()

        //var locationFlow: SharedFlow<Location?>? = this@ForegroundOnlyLocationService.locationFlow
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

    override fun onDestroy() {
        super.onDestroy()

        Log.d("Shay", "Destroyed")
    }
}

