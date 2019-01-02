package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintOnlineAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.SimulationAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility

class SimulationActivity : OnMapActivity() {

    lateinit var simulationAdapter: SimulationAdapter
    var simulationData : MutableList<JsonData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)

    }


    protected fun loadDatasetFromFile(filePath: String) {
        val dataset = JsonUtility.readDatasetFromFile(filePath)
        for(d in dataset.data!!) {
            var data:JsonData = d
            simulationData!!.add(data)
        }
    }

    fun runSimulation(algorithm : String) {

    }

    override fun displayMap(){// Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView
        img.setImageBitmap(bitmap)

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)

        // Drawing all the beacons for this map
        for (beacon in floorMap.savedBeacons) {
            val imageView = ImageView(this)
            setupResource(beacon, imageView)
        }

        // Drawing all the points of interest for this map
        for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }

        // Drawing all fingerprinting zones for this map
        for (zone in floorMap.fingerprintZones) {
            val imageView = ImageView(this)
            setupResource(zone, imageView)
        }
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
