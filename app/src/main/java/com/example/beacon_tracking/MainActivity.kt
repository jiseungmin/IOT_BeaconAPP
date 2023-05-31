package com.example.beacon_tracking

import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region

class MainActivity : AppCompatActivity() {
    private var ex  = 0
    private lateinit var beacon: BeaconManager
    private val MY_PERMISSION_ACCESS_ALL = 100
    private val DetectButton : Button by lazy { findViewById(R.id.DetectButton) }
    private val stateTextView : TextView by  lazy { findViewById(R.id.stateTextView) }
    private val region = Region("all-beacons-region", null, null, null)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        beacon =  BeaconManager.getInstanceForApplication(this)
        PermissionReq() // 권한 설정
        ScanButton()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode === MY_PERMISSION_ACCESS_ALL) {
            if (grantResults.size > 0) {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) System.exit(0)
                }
            }
        }
    }

    private fun startScan() {
        beacon.getRegionViewModel(region).regionState.observeForever(monitoringObserver)
        beacon.startMonitoring(region)
    }

    private val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            Log.d(TAG, "Beacons detected")
            stateTextView.text = "주변 물체가 감지되었습니다. "
        }
        else {
            Log.d(TAG, "No beacons detected")
            stateTextView.text = "주변 물체를 감지하지 못했습니다."
        }
    }

    private fun ScanButton(){
        DetectButton.setOnClickListener {
            if (ex == 0) {
                ex = 1
                DetectButton.setText("탐색 중지")
                stateTextView.setText("주변 물체를 찾고 있습니다.")
                startScan()
            } else {
                ex = 0
                DetectButton.setText("탐색 하기")
                stateTextView.setText("탐지를 진행하시겠습니까?")
                beacon.stopMonitoring(region)
            }
        }
    }

    private fun PermissionReq(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ||ActivityCompat.checkSelfPermission(this,android.Manifest.permission.BLUETOOTH_SCAN)!= PackageManager.PERMISSION_GRANTED){
            var permissions = arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.BLUETOOTH_SCAN)
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSION_ACCESS_ALL)
        }
    }
}