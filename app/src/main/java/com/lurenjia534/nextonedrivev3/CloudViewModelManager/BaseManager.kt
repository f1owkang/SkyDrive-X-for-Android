package com.lurenjia534.nextonedrivev3.CloudViewModelManager

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope

interface BaseManager {
    val viewModelScope: CoroutineScope
    val parentViewModel: ViewModel
}