package ar.edu.unicen.exa.bconmanager.Controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.beacon_icon
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import kotlinx.android.synthetic.main.activity_find_me.*
import kotlinx.android.synthetic.main.activity_my_beacons.*
import android.view.View.MeasureSpec
import android.graphics.drawable.Drawable
import android.provider.MediaStore.Images.Media.getBitmap
import android.opengl.ETC1.getWidth
import android.opengl.ETC1.getHeight
import android.graphics.drawable.BitmapDrawable
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import ar.edu.unicen.exa.bconmanager.R.mipmap.ic_launcher
import android.util.Log
import android.view.Window
import android.support.constraint.solver.widgets.WidgetContainer.getBounds
import android.R.attr.x
import android.graphics.*
import android.view.Display






class FindMeActivity : AppCompatActivity() {

    private val bluetoothScanner = BluetoothScanner()
    lateinit var testMap : CustomMap
    lateinit var devicesListAdapter : BeaconsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContentView(R.layout.activity_find_me)
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





        // Creating a map (TO DO: Display according to image)
        testMap = CustomMap(0, 3.0, 4.0) // in meters
        //val size = getBitmapPositionInsideImageView(floorPlan)


//        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.floor_plan)
//        val imageWidth = bitmap.width
//        val imageHeight = bitmap.height

        //floorPlan.scaleType
        testMap.calculateRatio(real_width, real_height)


        // TEST: Creating a test beacon and displaying it
        val testBeacon = BeaconOnMap(Location(0.0, 2.0, testMap), BeaconDevice("AA:BB:CC", 80, null))
        testBeacon.image = beacon_icon

        setupBeacon(testBeacon)

        devicesListAdapter = BeaconsAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)



    }

    private fun setupBeacon(testBeacon: BeaconOnMap) {

        // Set up the beacon's image size and position
        val imageView = ImageView(this)
        val layoutParams = LinearLayout.LayoutParams(100, 100) // value is in pixels

        layoutParams.leftMargin = testBeacon.position.getX() - 50
        layoutParams.topMargin = testBeacon.position.getY() - 50
        imageView.setImageResource(testBeacon.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)
    }

    fun refreshButtonClicked(view: View) {
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)
    }

    /**
     * Returns the bitmap position inside an imageView.
     * @param imageView source ImageView
     * @return 0: left, 1: top, 2: width, 3: height
     */
    fun getBitmapPositionInsideImageView(imageView: ImageView?): IntArray {
        val ret = IntArray(4)

        if (imageView == null || imageView.drawable == null)
            return ret

        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val d = imageView.drawable
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight

        // Calculate the actual dimensions
        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)

        ret[2] = actW
        ret[3] = actH

        // Get image position
        // We assume that the image is centered into ImageView
        val imgViewW = imageView.width
        val imgViewH = imageView.height

        val top = (imgViewH - actH) / 2
        val left = (imgViewW - actW) / 2

        ret[0] = left
        ret[1] = top

        return ret
    }


}
