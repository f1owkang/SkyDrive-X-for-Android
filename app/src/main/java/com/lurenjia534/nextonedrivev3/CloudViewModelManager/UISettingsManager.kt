package com.lurenjia534.nextonedrivev3.CloudViewModelManager

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import androidx.core.content.edit

class UISettingsManager(
    override val parentViewModel: ViewModel,
    override val viewModelScope: CoroutineScope,
    private val context: Context
) : BaseManager {

    // 深色模式状态
    private val _isDarkMode = MutableLiveData<Boolean>()
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    init {
        // 加载深色模式偏好设置
        loadDarkModePreference()
    }

    /**
     * 加载深色模式偏好设置
     */
    private fun loadDarkModePreference() {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        _isDarkMode.value = sharedPreferences.getBoolean("dark_mode", false)
    }

    /**
     * 保存深色模式偏好设置
     */
    fun saveDarkModePreference(isDarkMode: Boolean) {
        val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit() { putBoolean("dark_mode", isDarkMode) }
        _isDarkMode.value = isDarkMode
    }
}