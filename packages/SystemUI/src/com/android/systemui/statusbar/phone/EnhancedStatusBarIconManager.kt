package com.android.systemui.statusbar.phone

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.telephony.SignalStrength
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
import android.util.Log
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.statusbar.phone.ui.StatusBarIconController
import javax.inject.Inject

/**
 * Enhanced Status Bar Icon Manager
 * Handles dynamic color updates and animations for status bar icons
 */
@SysUISingleton
class EnhancedStatusBarIconManager @Inject constructor(
    private val context: Context,
    private val statusBarIconController: StatusBarIconController
) {
    
    private val handler = Handler(Looper.getMainLooper())
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    
    companion object {
        private const val TAG = "EnhancedStatusBarIcon"
        private const val BATTERY_LOW_THRESHOLD = 20
        private const val SIGNAL_EXCELLENT_THRESHOLD = 4
        private const val SIGNAL_GOOD_THRESHOLD = 3
        private const val SIGNAL_FAIR_THRESHOLD = 2
        private const val WIFI_EXCELLENT_THRESHOLD = -50
        private const val WIFI_GOOD_THRESHOLD = -60
        private const val WIFI_FAIR_THRESHOLD = -70
    }
    
    fun startMonitoring() {
        Log.d(TAG, "Starting enhanced status bar icon monitoring")
        
        // Register battery receiver
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
        }
        context.registerReceiver(batteryReceiver, batteryFilter)
        
        // Register WiFi receiver
        val wifiFilter = IntentFilter().apply {
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(wifiReceiver, wifiFilter)
        
        // Register Bluetooth receiver
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, bluetoothFilter)
        
        // Initial update
        updateAllIcons()
    }
    
    fun stopMonitoring() {
        Log.d(TAG, "Stopping enhanced status bar icon monitoring")
        try {
            context.unregisterReceiver(batteryReceiver)
            context.unregisterReceiver(wifiReceiver)
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receivers", e)
        }
    }
    
    private fun updateAllIcons() {
        updateBatteryIcon()
        updateWifiIcon()
        updateBluetoothIcon()
    }
    
    private fun updateBatteryIcon() {
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        
        when {
            batteryLevel <= BATTERY_LOW_THRESHOLD -> {
                statusBarIconController.setIconTint("battery", 
                    context.getColor(com.android.systemui.R.color.status_bar_battery_low))
                // Start low battery animation
                startLowBatteryAnimation()
            }
            isCharging -> {
                statusBarIconController.setIconTint("battery", 
                    context.getColor(com.android.systemui.R.color.status_bar_battery_charging))
                // Start charging animation
                startChargingAnimation()
            }
            else -> {
                statusBarIconController.setIconTint("battery", 
                    context.getColor(android.R.color.white))
                // Stop animations
                stopBatteryAnimations()
            }
        }
    }
    
    private fun updateWifiIcon() {
        val wifiInfo = wifiManager.connectionInfo
        val rssi = wifiInfo.rssi
        
        val colorRes = when {
            rssi >= WIFI_EXCELLENT_THRESHOLD -> com.android.systemui.R.color.status_bar_wifi_excellent
            rssi >= WIFI_GOOD_THRESHOLD -> com.android.systemui.R.color.status_bar_wifi_good
            rssi >= WIFI_FAIR_THRESHOLD -> com.android.systemui.R.color.status_bar_wifi_fair
            else -> com.android.systemui.R.color.status_bar_wifi_poor
        }
        
        statusBarIconController.setIconTint("wifi", context.getColor(colorRes))
    }
    
    private fun updateBluetoothIcon() {
        val colorRes = if (bluetoothAdapter.isEnabled) {
            com.android.systemui.R.color.status_bar_bluetooth_connected
        } else {
            com.android.systemui.R.color.status_bar_bluetooth_disconnected
        }
        
        statusBarIconController.setIconTint("bluetooth", context.getColor(colorRes))
    }
    
    private fun startLowBatteryAnimation() {
        // Apply low battery animation to battery icon
        statusBarIconController.setIconAnimation("battery", 
            com.android.systemui.R.anim.status_bar_icon_low_battery)
    }
    
    private fun startChargingAnimation() {
        // Apply charging animation to battery icon
        statusBarIconController.setIconAnimation("battery", 
            com.android.systemui.R.anim.status_bar_icon_charging)
    }
    
    private fun stopBatteryAnimations() {
        // Remove animations from battery icon
        statusBarIconController.setIconAnimation("battery", null)
    }
    
    // Broadcast receivers
    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.post { updateBatteryIcon() }
        }
    }
    
    private val wifiReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.post { updateWifiIcon() }
        }
    }
    
    private val bluetoothReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handler.post { updateBluetoothIcon() }
        }
    }
}
