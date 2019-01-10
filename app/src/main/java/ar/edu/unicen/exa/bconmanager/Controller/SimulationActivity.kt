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
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataset
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.Algorithm
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.PDRService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.ParticleFilterService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.TrilaterationService
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility

class SimulationActivity : OnMapActivity() {

    private val datasetPath = "/storage/emulated/0/Download/Dataset.json"
    private val datasetPathMod = "/storage/emulated/0/Download/Dataset2.json"
    private var simulationData: MutableList<JsonData> = mutableListOf()
    lateinit var algorithm: Algorithm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)

    }


    private fun loadDatasetFromFile(filePath: String) {
        val dataset = JsonUtility.readDatasetFromFile(filePath)
        for(d in dataset.data!!) {
            var data: JsonData = d
            simulationData.add(data)
        }
    }

    private fun saveDatasetToFile(filePath: String) {
        val dataset = JsonDataset(simulationData)
        JsonUtility.saveDatasetToFile(filePath, dataset)
        Log.d("SIMULATION", "Finished saving to $filePath")
    }


    fun runSimulationPF(view: View) {
        algorithm = ParticleFilterService()
        runSimulation(1)
    }
    fun runSimulationPDR(view : View) {
        algorithm =  PDRService.instance
        (algorithm as PDRService).setAdjustedBearing(floorMap.angle.toFloat())
        runSimulation(1)
    }

    fun runSimulationTrilat(view: View) {
        algorithm = TrilaterationService.instance
        runSimulation(1)
    }

    private fun runSimulation(choice: Number) {

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
            if ((i + 1) < simulationData!!.size) {
                nextTimestamp = simulationData!!.get(i + 1).timestamp
            } else
                nextTimestamp = currentData.timestamp
            var calculatedPosition = algorithm.getNextPosition(currentData,nextTimestamp)
            //Log.d("SIMULATION-f", "[$i] " + algorithm.getNextPosition(currentData,nextTimestamp).toString())
            Log.d("SIMULATION-f", "[$i] " + calculatedPosition.toString())
            algorithm.showError(Location(currentData.positionX,currentData.positionY,floorMap),calculatedPosition)
            currentData.estimateX = calculatedPosition.x
            currentData.estimateY = calculatedPosition.y
            Log.d("SIMULATION-f", "[$i] " + calculatedPosition.toString())
            i++
        }
        Log.d("SIMULATION", "Finished, lets save")
        saveDatasetToFile(datasetPathMod)
    }

    override fun displayMap() {// Loading the map from a JSON file
        super.displayMap()
        loadDatasetFromFile(datasetPath)
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
