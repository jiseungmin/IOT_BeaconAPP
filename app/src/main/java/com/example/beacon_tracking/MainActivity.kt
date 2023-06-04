package com.example.beacon_tracking

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.Region

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var Ex = 0
    private lateinit var naverMap: NaverMap
    private lateinit var Beacon: BeaconManager
    private val MY_PERMISSION_ACCESS_ALL = 100
    private lateinit var locationSource: FusedLocationSource
    private lateinit var sharedPreferences: SharedPreferences
    private val mapView : MapView by lazy { findViewById(R.id.mapView) }
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val Timmer: Chronometer by lazy { findViewById(R.id.Timemmer) }
    private val DetectButton: Button by lazy { findViewById(R.id.DetectButton) }
    private val StateTextView: TextView by lazy { findViewById(R.id.stateTextView) }
    private val region = Region("beacons", Identifier.parse("74278bda-b644-4520-8f0c-720eaf059935"), null, null) // 비콘정보  식별객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        locationSource = FusedLocationSource(this, MY_PERMISSION_ACCESS_ALL)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences("MarkerData", Context.MODE_PRIVATE)
        Beacon = BeaconManager.getInstanceForApplication(this)
        Beacon.getBeaconParsers().add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")) // 비콘파서는 비콘의 정보를 추출하는 역할을 하며 비콘레이아웃은 비콘에서 전송되는 데이터 패턴을 정의하는 역할을 함
        LocationPermission() // 권한 설정
        ScanButton() // 버튼
    }

    private fun LocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSION_ACCESS_ALL)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
        Ex = 1
        Timmer.isVisible = true
        Timmer.base = SystemClock.elapsedRealtime()
        Timmer.start()
        DetectButton.setText("탐색 중지")
        StateTextView.setText("주변 물체를 찾고 있습니다.")
        Beacon.getRegionViewModel(region).regionState.observeForever(monitoringObserver) // 비콘 객체 상태 관찰
        Beacon.startMonitoring(region)
    }

    private fun ScanButton() {
        DetectButton.setOnClickListener {
            if (Ex == 0) {
                startScan()
            } else {
                showConfirmationDialog()
            }
        }
    }

    private val monitoringObserver = Observer<Int> { state ->
        if (state == MonitorNotifier.INSIDE) {
            StateTextView.text = "주변 물체가 감지되었습니다. "
            Toast.makeText(this, "주변 물체와 거리가 가까워졌습니다.", Toast.LENGTH_SHORT).show()
        } else {
            StateTextView.text = "주변 물체를 감지하지 못했습니다."
            Toast.makeText(this, "주변 물체와 거리가 멀어졌습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Stop Detecting")
        alertDialogBuilder.setMessage("원하는 물체를 발견하였습니까?")
        alertDialogBuilder.setPositiveButton("예") { dialog, which ->
            Ex = 0
            Timmer.stop()
            Timmer.base = SystemClock.elapsedRealtime()
            Timmer.isVisible = false
            DetectButton.setText("탐색 하기")
            StateTextView.setText("탐지를 진행하시겠습니까?")
            Beacon.stopMonitoring(region)
            Beacon.getRegionViewModel(region).regionState.removeObservers(this)
            Locations()
        }
        alertDialogBuilder.setNegativeButton("아니오") { dialog, which -> {} }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }


    private fun Locations(){
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val marker = Marker() // 마커 생성
                    marker.position = LatLng(latitude, longitude) // 현재 위치로 설정
                    marker.map = naverMap // Naver 지도에 마커 추가
                    marker.icon = MarkerIcons.BLACK
                    marker.iconTintColor = Color.RED

                    val editor = sharedPreferences.edit()
                    editor.putFloat("latitude", latitude.toFloat())
                    editor.putFloat("longitude", longitude.toFloat())
                    editor.apply()
                }
            }
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map
        naverMap.minZoom = 5.0
        naverMap.maxZoom = 18.0
        val cameraUpdate = CameraUpdate.scrollTo(LatLng( 36.79914699999987, 127.07452627903502))
        naverMap.moveCamera(cameraUpdate) // 초기 위치 설정
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.Follow

        val latitude = sharedPreferences.getFloat("latitude", 0f).toDouble()
        val longitude = sharedPreferences.getFloat("longitude", 0f).toDouble()
        if (latitude != 0.0 && longitude != 0.0) {
            val marker = Marker() // 마커 생성
            marker.position = LatLng(latitude, longitude) // 저장된 위치로 설정
            marker.map = naverMap // Naver 지도에 마커 추가
            marker.icon = MarkerIcons.BLACK
            marker.iconTintColor = Color.RED
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}