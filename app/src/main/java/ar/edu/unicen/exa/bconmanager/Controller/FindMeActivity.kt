package ar.edu.unicen.exa.bconmanager.Controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.beacon_icon
import kotlinx.android.synthetic.main.activity_find_me.*


class FindMeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_me)

        // TEST: Creating a test beacon and displaying it
        val testBeacon = BeaconOnMap(Location(500.toDouble(), 300.toDouble()), BeaconDevice("AA:BB:CC", 80, null))
        testBeacon.image = beacon_icon

        setupBeacon(testBeacon)
    }

    private fun setupBeacon(testBeacon: BeaconOnMap) {

        // Set up the beacon's image size and position
        val imageView = ImageView(this)
        val layoutParams = LinearLayout.LayoutParams(100, 100) // value is in pixels
        layoutParams.leftMargin = testBeacon.position.x.toInt() - 50
        layoutParams.topMargin = testBeacon.position.y.toInt() - 50
        imageView.setImageResource(testBeacon.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)
    }

    fun refreshButtonClicked(view: View) {

    }
}
