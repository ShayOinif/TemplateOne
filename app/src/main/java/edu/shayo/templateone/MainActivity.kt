package edu.shayo.templateone

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import edu.shayo.templateone.databinding.ActivityMainBinding
import edu.shayo.templateone.permissions.Permission
import edu.shayo.templateone.permissions.PermissionRequester
import edu.shayo.templateone.service.ForegroundOnlyLocationService
import edu.shayo.templateone.service.LocationServiceCoroutineScope
import edu.shayo.templateone.utils.SharedPreferenceUtil
import edu.shayo.templateone.utils.SharedPreferenceUtil.PREFERENCE_FILE_KEY
import edu.shayo.templateone.utils.toText
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var foregroundOnlyLocationServiceBound = false

    private var foregroundOnlyLocationService: ForegroundOnlyLocationService.LocalBinder? = null

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var foregroundOnlyLocationButton: Button

    private var _binding: ActivityMainBinding? = null
    private val binding
        get() = _binding!!

    @Inject
    lateinit var permissionRequester: PermissionRequester

    @Inject
    lateinit var locationServiceCoroutineScope: LocationServiceCoroutineScope

    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as ForegroundOnlyLocationService.LocalBinder
            foregroundOnlyLocationService = binder
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            foregroundOnlyLocationService = null
            foregroundOnlyLocationServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        sharedPreferences =
            getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)

        foregroundOnlyLocationButton = findViewById(R.id.foreground_only_location_button)

        permissionRequester.from(this)

        foregroundOnlyLocationButton.setOnClickListener {
            val enabled = sharedPreferences.getBoolean(
                SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false
            )

            if (enabled) {
                foregroundOnlyLocationService?.unsubscribeToLocationUpdates()
            } else {
                lifecycleScope.launch {
                    permissionRequester
                        .withPermissions(
                            Permission.FineLocation,
                            Permission.BackgroundLocation,
                            Permission.ForegroundService
                        )
                        ?.checkPermissions { grantedMap ->
                            grantedMap[Permission.FineLocation]?.let { fineLocationGranted ->
                                if (!fineLocationGranted)
                                    makePermissionDenialSnackbar(Permission.FineLocation)
                                else {

                                    val canRunInTheBackground = if (
                                        grantedMap[Permission.BackgroundLocation] == false &&
                                        grantedMap[Permission.ForegroundService] == false
                                    ) {
                                        makePermissionDenialSnackbar(Permission.BackgroundLocation)

                                        false
                                    } else {
                                        true
                                    }

                                    foregroundOnlyLocationService?.subscribeToLocationUpdates(
                                        canRunInTheBackground
                                    )
                                }
                            }
                        }
                }
            }
        }

            lifecycleScope.launchWhenResumed {
                repeatOnLifecycle(Lifecycle.State.RESUMED) {
                ForegroundOnlyLocationService.locationFlow.collect {
                    logResultsToScreen(it.toText())
                }
            }
        }
    }

    private fun makePermissionDenialSnackbar(permission: Permission) {
        Snackbar.make(
            binding.root,
            getString(
                R.string.permission_denied_explanation,
                permission.permissionRationaleResId
            ),
            Snackbar.LENGTH_LONG
        )
    }

    override fun onStart() {
        super.onStart()

        updateButtonState(
            sharedPreferences.getBoolean(SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false)
        )

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        if (foregroundOnlyLocationServiceBound) {
            unbindService(foregroundOnlyServiceConnection)
            foregroundOnlyLocationServiceBound = false
        }

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)

        super.onStop()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) {
            updateButtonState(
                sharedPreferences.getBoolean(
                    SharedPreferenceUtil.KEY_FOREGROUND_ENABLED, false
                )
            )
        }
    }

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            foregroundOnlyLocationButton.text =
                getString(R.string.stop_location_updates_button_text)
        } else {
            foregroundOnlyLocationButton.text =
                getString(R.string.start_location_updates_button_text)
        }
    }

    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs = "$output\n${binding.outputTextView.text}"
        binding.outputTextView.text = outputWithPreviousLogs
    }

    override fun onDestroy() {
        super.onDestroy()

        _binding = null
    }
}