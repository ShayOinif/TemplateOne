package edu.shayo.templateone.service

import android.app.PendingIntent
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import edu.shayo.templateone.MainActivity
import edu.shayo.templateone.R
import edu.shayo.templateone.notifications.NotificationManager
import edu.shayo.templateone.utils.SharedPreferenceUtil
import edu.shayo.templateone.utils.toText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import java.util.*
import javax.inject.Inject

private const val PACKAGE_NAME = "edu.shayo.templateone"
private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION =
    "$PACKAGE_NAME.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"

@AndroidEntryPoint
class ForegroundOnlyLocationService : LifecycleService() {

    private var configurationChange = false

    private var serviceRunningInForeground = false

    private val localBinder = LocalBinder()

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var locationRequest: LocationRequest

    private var locationCallback: LocationCallback? = null

    private var currentLocation: Location? = null

    private val notificationChannelId = "$PACKAGE_NAME.${UUID.randomUUID()}"

    private val notificationId = (10000000..12345678).random()

    private var locationFlow: SharedFlow<Location?>? = null

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = 10_000
            fastestInterval = 5_000
            maxWaitTime = 20_000
            // TODO: Check alternative ,priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    // Implementation of a cold flow backed by a Channel that sends Location updates
    private fun FusedLocationProviderClient.locationFlow(request: LocationRequest) = callbackFlow {
        val innerCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                try {

                    currentLocation = result.lastLocation

                    if (serviceRunningInForeground) {
                        notificationManager.notify(
                            notificationId,
                            notificationManager.generateNotification(
                                currentLocation?.toText() ?: getString(R.string.no_location_text),
                                getString(R.string.app_name),
                                getListOfAction(),
                                notificationChannelId
                            )
                        )
                    } else {
                        trySend(currentLocation).isSuccess
                    }
                } catch(e: Exception) {

                }
            }
        }

        locationCallback = innerCallback

        requestLocationUpdates(request, innerCallback, Looper.getMainLooper())
            .addOnFailureListener { e ->
                close(e) // in case of exception, close the Flow
            }
        // clean up when Flow collection ends
        awaitClose {
            removeLocationUpdates(innerCallback)
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
                currentLocation?.toText() ?: getString(R.string.no_location_text),
                getString(R.string.app_name),
                getListOfAction(),
                notificationChannelId
            )

            startForeground(notificationId, notification)
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

        try {

            locationFlow = fusedLocationProviderClient.locationFlow(locationRequest).shareIn(lifecycleScope, SharingStarted.Eagerly, 0)

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        }
    }

    private fun unsubscribeToLocationUpdates() {
        try {
            locationCallback?.let {
                val removeTask = fusedLocationProviderClient.removeLocationUpdates(it)
                removeTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        stopSelf()
                    }
                }
            }

            SharedPreferenceUtil.saveLocationTrackingPref(this, false)

        } catch (unlikely: SecurityException) {
            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
        }
    }

    inner class LocalBinder : Binder() {
        fun subscribeToLocationUpdates() = this@ForegroundOnlyLocationService.subscribeToLocationUpdates()

        fun unsubscribeToLocationUpdates() = this@ForegroundOnlyLocationService.unsubscribeToLocationUpdates()

        var locationFlow = this@ForegroundOnlyLocationService.locationFlow
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

        val serviceAction =  NotificationCompat.Action(
            R.drawable.ic_cancel,
            getString(R.string.stop_location_updates_button_text),
            servicePendingIntent
        )


        return listOf(
            activityAction,
            serviceAction
        )
    }
}

