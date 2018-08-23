package ar.edu.unicen.exa.bconmanager.Controller


import android.Manifest
import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler.StepDetectionListener

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.model.LatLng

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Point
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.*

import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import ar.edu.unicen.exa.bconmanager.Service.StepPositioningHandler
import kotlinx.android.synthetic.main.activity_find_me.*
import java.math.BigDecimal

class PDRActivity : AppCompatActivity() {

    private var sm: SensorManager? = null
    private var sdh: StepDetectionHandler? = null
    private var sph: StepPositioningHandler? = null
    private var dah: DeviceAttitudeHandler? = null
    private var isWalking = false
    private var lKloc: Location? = null
    private var lastKnown: LatLng? = null
    private var filePath: String = ""
    private val  TAG = "PDRActivity"
    private lateinit var floorMap: CustomMap
    lateinit var positionView: ImageView
    private var startingPoint = false
    private lateinit var startingLocation : ar.edu.unicen.exa.bconmanager.Model.Location

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr) //CHANGE LAYOUT

        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            101 -> if (resultCode == -1) {
                val uri = data!!.data
                filePath = uri.lastPathSegment.removePrefix("raw:")
            }
        }
        if (!filePath.isNullOrEmpty()) {
            displayMap()
        } else {
            Log.e(TAG, "The file path is incorrect")
        }
    }

    fun  getLocation(): Location? {
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

    private fun displayMap() {
        Log.d(TAG,"DISPLAY MAP")

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        Log.d("FILEPATH",filePath)
        // Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView //CHECK
        img.setImageBitmap(bitmap)
        img.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent):Boolean {
                if(!startingPoint) {
                    val screenX = event.x
                    val screenY = event.y
                    val viewX = screenX - v.left
                    val viewY = screenY - v.top
                    Log.d(TAG, "Touching x: $viewX y: $viewY")
                    // check if point exists

                    // if it does, click it
                    // otherwise, create a new point there
                    createFingerprintingPoint(viewX, viewY)
                    startingPoint = true
                }
                return false
            }
        })

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)


        // Drawing all the points of interest for this map
        for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }


        if (servicesConnected()) {
            lKloc = getLocation()
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

    private fun loadMapFromFile(filePath: String): CustomMap {
        val jsonMap = JsonUtility.readFromFile(filePath)
        val fileMap = CustomMap("", 0.0, 0.0)
        fileMap.startFromFile(jsonMap)
        Log.d(TAG, "Map loaded from JSON file in $filePath")
        return fileMap
    }



    private fun getRealMapSize(): Point {
        val realSize = Point()
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val width = size.x
        realSize.x = width
        realSize.y = floorPlan.drawable.intrinsicHeight * width / floorPlan.drawable.intrinsicWidth
        return realSize
    }

    private fun printDisplayProperties() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        Log.d("POSITION", "TOTAL WIDTH IS : $width")
        Log.d("POSITION", "TOTAL HEIGHT IS : $height")
        Log.d("POSITION", "-----------------------------")
        Log.d("POSITION", "WIDTH IS : ${floorPlan.drawable.intrinsicWidth}")
        Log.d("POSITION", "HEIGHT IS :  ${floorPlan.drawable.intrinsicHeight}")
        val real_width = width
        val real_height = floorPlan.drawable.intrinsicHeight * width / floorPlan.drawable.intrinsicWidth
        Log.d("POSITION", "-----------------------------")
        Log.d("POSITION", "WIDTH IS : $width")
        Log.d("POSITION", "HEIGHT IS :  ${real_height}")
        Log.d("POSITION", "-----------------------------")

    }

    private fun setupResource(resource: Resource, imageView: ImageView) {

        // Set up the resource image size and position
        val layoutParams: LinearLayout.LayoutParams
        if (resource is PointOfInterest) {
            val loc = Location(resource.zone * 2, resource.zone * 2, floorMap)
            layoutParams = LinearLayout.LayoutParams(loc.getX(), loc.getY()) // value is in pixel
        } else {
            layoutParams = LinearLayout.LayoutParams(70, 70) // value is in pixels
        }
        layoutParams.leftMargin = resource.position.getX() - (layoutParams.width / 2)
        layoutParams.topMargin = resource.position.getY() - (layoutParams.height / 2)
        imageView.setImageResource(resource.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)

    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "Touching ${event.toString()}")
        return super.onTouchEvent(event)
    }


    private fun servicesConnected(): Boolean {
        val resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this)
        return if (ConnectionResult.SUCCESS == resultCode) {
            true
        } else false
    }

    override fun onResume() {
        super.onResume()
        if((sdh != null) && (dah != null)) {
            sdh!!.start()
            dah!!.start()
        }

    }

    override fun onPause() {
        super.onPause()
        if((sdh != null) && (dah != null)) {
            sdh!!.stop()
            dah!!.stop()
        }
    }
    private fun createFingerprintingPoint(viewX: Float, viewY: Float) {
        val loc = Location(0.0, 0.0, floorMap)
        loc.setX(viewX.toInt())
        loc.setY(viewY.toInt())
        val zone = FingerZone(loc)
        startingLocation = loc
        Log.d(TAG,"Starting Location ${startingLocation.toString()}")
        Log.d(TAG, "Touching ${zone.toString()}")
        val imageView = ImageView(this)
        zone.image = R.drawable.zone_icon
        setupResource(zone, imageView)

    }

}