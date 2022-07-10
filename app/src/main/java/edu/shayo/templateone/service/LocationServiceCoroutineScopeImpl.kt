package edu.shayo.templateone.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

class LocationServiceCoroutineScopeImpl @Inject constructor() : LocationServiceCoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()
}