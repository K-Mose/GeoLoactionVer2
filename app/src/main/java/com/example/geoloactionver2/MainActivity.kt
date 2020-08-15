package com.example.geoloactionver2


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    // 도움
    // https://medium.com/@manuaravindpta/getting-current-location-in-kotlin-30b437891781
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private  lateinit var task: Task<LocationSettingsResponse>

    companion object {
        val TAG = "GoogleLocationServices :: "
        var latitude:Double = 0.0
        var longitude:Double = 0.0
        val REQUEST_PERMISSIONS_REQUEST_CODE = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var mGeoCoder = Geocoder(applicationContext, Locale.KOREAN)
        var mResultList : List<Address>? = null

        // 위치 설저 확인
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(this)
            // android Task<T> : https://developer.android.com/reference/com/google/android/play/core/tasks/Task
        task= client.checkLocationSettings(builder.build())

        task.addOnCompleteListener{
            if(it.isSuccessful){
                val locationSettingsStates = task.result
                // Location Settings States Options : https://developers.google.com/android/reference/com/google/android/gms/location/LocationSettingsStates
                if(!locationSettingsStates!!.locationSettingsStates.isLocationUsable){
                    Log.d(TAG, "!locationSettingsStates.isLocationUsable")
                    LatAndLong!!.setText("위치 정보 없음")
                    // Dialog
                    // https://g-y-e-o-m.tistory.com/47
                    AlertDialog.Builder(this)
                        .setTitle("위치 정보 활성화")
                        .setMessage("위치 정보 활성화가 필요합니다.")
                        .setNeutralButton("설정", DialogInterface.OnClickListener { dialog, which ->
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                            startLocationUpdates("here")
                        })
                        .setPositiveButton("거절", DialogInterface.OnClickListener { dialog, which ->
                            finish()
                        })
                        .setCancelable(false)
                        .create()
                        .show()
                }else{
                    getLastLocation()
                }
            }else
            {
                Log.d(TAG, "Fail to create task")
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()

        locationRequest = LocationRequest.create()
        locationRequest.run{
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 6000
        }
        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                if(longitude != 0.0 && latitude != 0.0){
                    Log.d(TAG, "위치 생성 완료, 콜백 종료")
                    stopLocationUpdates()
                }else{
                    locationResult ?: return
                    Log.d(TAG, "locationResult - ${locationResult.locations}")
                    for (location in locationResult.locations){
                        longitude = location.longitude
                        latitude = location.latitude
                    }
                    LatAndLong!!.setText("lat : ${latitude}   long : ${longitude} ")
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,locationCallback,Looper.getMainLooper())


        getLocation.setOnClickListener {
            getLastLocation()
            Toast.makeText(this, "$latitude, $longitude", Toast.LENGTH_SHORT).show()
            if(longitude != 0.0 && latitude != 0.0){
                try{
                    mResultList = mGeoCoder.getFromLocation(
                        latitude!!.toDouble() , longitude!!.toDouble(), 1
                    )
                }catch (e :Exception){
                    e.printStackTrace()
                }
                if(mResultList != null){
                    Log.d(TAG, "CurrentLocation : ${mResultList!![0].getAddressLine(0)}")
                    address.setText("CurrentLocation : ${mResultList!![0].getAddressLine(0)}")
                }
            }
        }
        fusedLocationClient.lastLocation
    }

    override fun onStart() {
        super.onStart()
        if(!checkPermissions()){
            requestPermissions()
        }else{
            getLastLocation()
        }
    }

    private fun getLastLocation(){
        fusedLocationClient!!.lastLocation
            .addOnCompleteListener(this){
                task ->
                if(task.isSuccessful && task.result != null){
                    lastLocation = task.result
                    longitude = lastLocation!!.longitude
                    latitude = lastLocation!!.latitude
                    LatAndLong!!.setText("lat : $latitude   long : $longitude ")
                }else{
                    Log.w(TAG, "getLastLocation:Exception :: ${task.exception}", task.exception)
                    Log.d(TAG, "getLastLocation:Exception :: ${task.isSuccessful}")
                    Log.d(TAG, "getLastLocation:Exception :: ${task.result}")
                    lastLocation = task.result
                    Log.d(TAG, "getLastLocation:Exception :: ${lastLocation}")

                    startLocationUpdates("getLastLocation")
                }
            }
    }

    // 권한 확인 반환
    private fun checkPermissions():Boolean {
           val permissionState =  ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    // 권한 요청
    private fun startLocationPermissionRequest(){
        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSIONS_REQUEST_CODE)

    }

    // 권한 확인 후 요청
    private fun requestPermissions(){
        val shouldProvideRelationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if(shouldProvideRelationale){
            Log.i(TAG, "DIsplaying permission rationale to provide additional context.")
            startLocationPermissionRequest()
        }else{
            Log.i(TAG, "Requesting permission")
            startLocationPermissionRequest()
        }
    }

    // 권한 설정 응답 받으면
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // 설정된 리퀘스트 코드로 확인한다.
        if(requestCode == REQUEST_PERMISSIONS_REQUEST_CODE){
            if(grantResults.size <= 0){
                // 권한 요청 취소 됐을 때
                Log.i(TAG, "User interaction was cancelled.")
            }else if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getLastLocation()
            }else{
                // 권한 요청 거절, 다시 요청
                startLocationPermissionRequest()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("google onResume :","onResume the app")
        startLocationUpdates("onResume")
    }

    private fun startLocationUpdates(where:String) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d(TAG, "function started at ${where}")
    }


    // 업데이트 중지
    private fun stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}
