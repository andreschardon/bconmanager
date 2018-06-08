package ar.edu.unicen.exa.bconmanager.Controller

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.R


class BeaconManagerActivity : AppCompatActivity() {

    private val permissions = arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission
            .BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon_manager)
    }

    override fun onResume() {
        super.onResume()
        setupPermissions()
        setupBluetooth()
        setupLocation()
    }

    private fun setupBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }
    }

    private fun setupLocation() {
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!locationEnabled) {
            val enableLocIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show()
            startActivity(enableLocIntent);
        }

    }

    private fun setupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(arePermissionsEnabled()){
//                    permissions granted, continue flow normally
            }else{
                requestMultiplePermissions();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(permissions[i])) {
                        AlertDialog.Builder(this)
                                .setMessage("Your error message here")
                                .setPositiveButton("Allow", { dialog, which -> requestMultiplePermissions() })
                                .setNegativeButton("Cancel", { dialog, which -> dialog.dismiss() })
                                .create()
                                .show()
                    }
                    return
                }
            }
            //all is good, continue flow
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun arePermissionsEnabled(): Boolean {
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun requestMultiplePermissions() {
        val remainingPermissions = mutableListOf<String>()
        for (permission in permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission)
            }
        }
        requestPermissions(remainingPermissions.toTypedArray(), 101)
    }

    fun goToMyBeaconsActivity(view : View) {
        val intent = Intent(this, MyBeaconsActivity::class.java)
        startActivity(intent)
    }

    fun goToFindMeActivity(view : View) {
        val intent = Intent(this, FindMeActivity::class.java)
        startActivity(intent)
    }
}
