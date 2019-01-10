package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.Algorithm
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.PDRService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.ParticleFilterService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.TrilaterationService
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility

class SimulationActivity : OnMapActivity() {

    var simulationData : MutableList<JsonData> = mutableListOf()
    lateinit var algorithm : Algorithm

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
            var data: JsonData = d
            simulationData.add(data)
        }
    }
    fun runSimulationPF(view : View) {
        algorithm =  ParticleFilterService()
        runSimulation(1)
    }
    fun runSimulationPDR(view : View) {
        algorithm =  PDRService.instance
        (algorithm as PDRService).setAdjustedBearing(floorMap.angle.toFloat())
        runSimulation(1)
    }
    fun runSimulationTrilat(view : View) {
        algorithm =  TrilaterationService.instance
        runSimulation(1)
    }
    fun runSimulation (choice : Number) {

        /*when(choice) {
            1 -> algorithm = PDRService.instance
            2 -> algorithm = TrilaterationService.instance
            else
                -> algorithm =  ParticleFilterService()
        }*/
        algorithm.startUp(floorMap)
        var i = 0
        //Log.d("SIMULATION", "Size is ${simulationData!!.size}")
        while (i <simulationData.size) {
            var currentData = simulationData[i]
            //Log.d("SIMULATION", currentData.toString())
            var nextTimestamp: Number = 0
            if ((i+1) < simulationData!!.size){
                nextTimestamp = simulationData!!.get(i+1).timestamp
            }
            else
                nextTimestamp = currentData.timestamp
            var calculatedPosition = algorithm.getNextPosition(currentData,nextTimestamp)
            //Log.d("SIMULATION-f", "[$i] " + algorithm.getNextPosition(currentData,nextTimestamp).toString())
            Log.d("SIMULATION-f", "[$i] " + calculatedPosition.toString())
            algorithm.showError(Location(currentData.posX,currentData.posY,floorMap),calculatedPosition)
            i++
        }

    }



    override fun displayMap(){// Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)
        loadDatasetFromFile("/storage/emulated/0/Download/Dataset.json")
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
