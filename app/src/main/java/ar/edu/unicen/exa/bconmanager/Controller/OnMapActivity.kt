package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.nfc.Tag
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import kotlinx.android.synthetic.main.activity_fingerprint_offline.*
import java.math.BigDecimal

abstract class OnMapActivity  : AppCompatActivity() {

    protected val bluetoothScanner = BluetoothScanner.instance
    protected var filePath: String = ""
    protected var datasetPath = ""
    protected open var TAG : String = ""
    protected lateinit var floorMap: CustomMap
    protected var touchListener : View.OnTouchListener? = null
    protected var datasetChosen = false
    protected var isChoosingFile = false

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()

    protected fun setupResource(resource: Resource, imageView: ImageView, width: Int = 70, height: Int = 70) {

        // Set up the resource image size and position
        val layoutParams: LinearLayout.LayoutParams
        if (resource is PointOfInterest) {
            val loc = Location(resource.zone * 2, resource.zone * 2, floorMap)
            layoutParams = LinearLayout.LayoutParams(loc.getX(), loc.getY()) // value is in pixel
        } else {
            layoutParams = LinearLayout.LayoutParams(width, height) // value is in pixels
        }
        if (resource is FingerprintZone) {
            resource.view = imageView
        }
        layoutParams.leftMargin = resource.position.getX() - (layoutParams.width / 2)
        layoutParams.topMargin = resource.position.getY() - (layoutParams.height / 2)
        imageView.setImageResource(resource.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)
    }

    protected fun removeResource(imageView: ImageView) {
        floorLayout.removeView(imageView);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            101 -> if (resultCode == -1) {
                val uri = data!!.data
                filePath = uri.lastPathSegment.removePrefix("raw:")
            }
            102 -> if (resultCode == -1) {
                val uri = data!!.data
                datasetChosen = true
                datasetPath = uri.lastPathSegment.removePrefix("raw:")
            }
        }
        if (!filePath.isNullOrEmpty()) {
            displayMap()
        } else {
            Log.e(TAG, "The file path is incorrect")
        }
    }

    protected open fun displayMap() {
        // Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView
        img.setImageBitmap(bitmap)
        if (this.touchListener != null)
            img.setOnTouchListener(touchListener)

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)
    }

    abstract fun updatePosition(beacons: List<BeaconDevice>)


    protected fun saveMapToFile(testMap: CustomMap, filePath: String) {
        val jsonMap = testMap.toJson()
        JsonUtility.saveToFile(filePath, jsonMap)
        Log.d(TAG, "Map saved to JSON file in $filePath")
    }

    protected fun loadMapFromFile(filePath: String): CustomMap {
        val jsonMap = JsonUtility.readFromFile(filePath)
        val fileMap = CustomMap("", 0.0, 0.0, 0.0)
        fileMap.startFromFile(jsonMap)
        Log.d(TAG, "Map loaded from JSON file in $filePath")
        return fileMap
    }


    protected fun getRealMapSize(): Point {
        val realSize = Point()
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val width = size.x
        realSize.x = width
        realSize.y = floorPlan.drawable.intrinsicHeight * width / floorPlan.drawable.intrinsicWidth
        return realSize
    }

}