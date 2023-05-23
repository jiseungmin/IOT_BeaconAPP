package com.example.beacon_tracking

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region

class MainActivity : AppCompatActivity() {
    private val beacon : BeaconManager =  BeaconManager.getInstanceForApplication(this)
    private val region = Region("all-beacons-region", null, null, null)
    private val DetectButton : Button by lazy {
        findViewById(R.id.DetectButton)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DetectButton.setOnClickListener {
            DetectButton.setText("탐색 중지")
            startScan()
        }
    }


    private fun startScan() {
        beacon.getRegionViewModel(region).regionState.observeForever(monitoringObserver)
        beacon.startMonitoring(region)
    }

    private val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            Log.d(TAG, "Beacons detected")
        }
        else {
            Log.d(TAG, "No beacons detected")
        }
    }


}