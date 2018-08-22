package ar.edu.unicen.exa.bconmanager.Controller


import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler.StepDetectionListener

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.model.LatLng

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.util.Log

import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.StepPositioningHandler

class PDRActivity : FragmentActivity  /*implements OnMapReadyCallback*/() {

    internal var sm: SensorManager? = null
    internal var sdh: StepDetectionHandler? = null
    internal var sph: StepPositioningHandler? = null
    internal var dah: DeviceAttitudeHandler? = null
    internal var isWalking = false
    internal var lKloc: Location? = null
    internal var lastKnown: LatLng? = null


    /** R�cup�re la derni�re position connue de l'utilisateur  */
    // TODO: Consider calling
    //    ActivityCompat#requestPermissions
    // here to request the missing permissions, and then overriding
    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
    //                                          int[] grantResults)
    // to handle the case where the user grants the permission. See the documentation
    // for ActivityCompat#requestPermissions for more details.
    val location: Location?
        get() {
            Log.d("SM", "ADENTRO DE GET LOCATION")
            val locationManager = this
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager

            if (locationManager != null) {
                Log.d("SM", locationManager.toString())
                Log.d("SM", "LOCATION NO ES NULL")
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return null
                }
                Log.d("SM", "GET LOCATION BEFORE LAST KNOWN LOCATIOON")
                val lastKnownLocationGPS = locationManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                Log.d("SM", "GET LOCATION AFTER LAST KNOWN LOCATIOON")
                if (lastKnownLocationGPS != null) {
                    Log.d("SM", "GET LOCATION LASTKNOWN LOCATION GPS")
                    Log.d("SM", "lastKnownLocationGPS: $lastKnownLocationGPS")
                    return lastKnownLocationGPS
                } else {
                    val loc = locationManager
                            .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    Log.d("SM", "LAST KNOWN GPS NULL, LOC: $loc")
                    return loc
                }
            } else {
                Log.d("SM", "LOCATION ES NULL")
                return null
            }
        }

    private val mStepDetectionListener = StepDetectionListener { stepSize ->
        val newloc = sph!!.computeNextStep(stepSize, dah!!.orientationVals[0])
        Log.d("LATLNG", newloc.latitude.toString() + " " + newloc.longitude + " " + dah!!.orientationVals[0])
        if (isWalking) {
            //update position on map
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr)
        if (servicesConnected()) {
            lKloc = location
            Log.d("SM", "lKloc: " + lKloc!!)
            lastKnown = LatLng(lKloc!!.latitude,
                    lKloc!!.longitude)
            Log.d("SM", "AFTER lAST KNOWN ASSIGNED, LastK: $lastKnown")
        }
        Log.d("SM", "ANTES DE SENSOR MANAGER")
        val sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sdh = StepDetectionHandler(sm)
        sdh!!.setStepListener(mStepDetectionListener)
        dah = DeviceAttitudeHandler(sm)
        sph = StepPositioningHandler()
        sph!!.setmCurrentLocation(lKloc)

    }

    /** verifier la disponibilit� des services google  */
    private fun servicesConnected(): Boolean {
        val resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this)
        return if (ConnectionResult.SUCCESS == resultCode) {
            true
        } else false
    }

    override fun onResume() {
        super.onResume()
        sdh!!.start()
        dah!!.start()
    }

    override fun onPause() {
        super.onPause()
        sdh!!.stop()
        dah!!.stop()
    }
}