package edu.shayo.templateone.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LocationServiceCoroutineScopeImpl : LocationServiceCoroutineScope {
    override val coroutineContext = Dispatchers.IO + SupervisorJob()
}