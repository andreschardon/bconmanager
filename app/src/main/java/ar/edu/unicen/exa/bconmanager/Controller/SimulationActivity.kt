package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataset
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonSimResult
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonTimestamp
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.*
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import kotlinx.android.synthetic.main.activity_simulation.*

class SimulationActivity : OnMapActivity() {

    private val datasetPath = "/storage/emulated/0/Download/Dataset.json"
    private val datasetPathMod = "/storage/emulated/0/Download/Results-"
    private var simulationData: MutableList<JsonData> = mutableListOf()
    lateinit var algorithm: Algorithm

    lateinit var algorithmFingerp : FingerprintingService
    lateinit var algorithmTrilat: TrilaterationService
    lateinit var algorithmPDR: PDRService
    lateinit var algorithmPF: ParticleFilterService


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

    private fun saveResultsToFile(filePath: String, result: JsonSimResult) {
        JsonUtility.saveResultToFile(filePath, result)
        Log.d("SIMULATION", "Finished saving to $filePath")
    }


    fun runSimulationPF(view: View) {
        algorithm = ParticleFilterService()
        runSimulation("ParticleFilter")
    }
    fun runSimulationPDR(view : View) {
        algorithm =  PDRService.instance
        (algorithm as PDRService).setAdjustedBearing(floorMap.angle.toFloat())
        runSimulation("PDR")
    }

    fun runSimulationTrilat(view: View) {
        algorithm = TrilaterationService.instance
        runSimulation("Trilat")
    }

    fun runSimulationFingerprinting(view: View) {
        algorithm = FingerprintingService()
        runSimulation("Fingerprint")
    }

    fun runFPTrilatSimulation(view: View) {
        algorithm = FPTrilat()
        runSimulation("FPTrilat")
    }

    fun runSimulationAll(view: View) {
        algorithmFingerp = FingerprintingService()
        algorithmTrilat = TrilaterationService.instance
        algorithmPDR = PDRService.instance
        algorithmPDR.setAdjustedBearing(floorMap.angle.toFloat())
        algorithmPF = ParticleFilterService()
        runSimulation("All")
    }

    private fun runSimulation(choice: String) {
        var maxError = 0.0
        var averageError = 0.0
        var result = JsonSimResult()
        var timestampList : MutableList<JsonTimestamp> = mutableListOf()

        algorithm.startUp(floorMap)
        var i = 0
        //Log.d("SIMULATION", "Size is ${simulationData!!.size}")
        var errorSum = 0.0

        while (i < simulationData.size) {
            val currentData = simulationData[i]
            //Log.d("SIMULATION", currentData.toString())
            var nextTimestamp: Number = 0
            if ((i + 1) < simulationData!!.size) {
                nextTimestamp = simulationData!!.get(i + 1).timestamp
            } else
                nextTimestamp = currentData.timestamp
            val calculatedPosition = algorithm.getNextPosition(currentData, nextTimestamp)
            Log.d("SIMULATION-f", "[$i] " + calculatedPosition.toString())
            val realPosition = Location(currentData.positionX, currentData.positionY, floorMap)

            // Calculate error
            val error = algorithm.getError(realPosition, calculatedPosition)
            errorSum += error
            if (error >= maxError)
                maxError = error

            var timestamp = JsonTimestamp(currentData.timestamp, currentData.positionX, currentData.positionY, error, calculatedPosition.x, calculatedPosition.y)
            timestampList.add(timestamp)

            if (algorithm is ParticleFilterService) {
                printPfLocations(algorithm as ParticleFilterService, realPosition)
            }
            i++
        }
        averageError = errorSum / simulationData.size
        result.timestamps = timestampList
        result.errorMax = maxError
        result.errorAverage = averageError
        val finalPath = "$datasetPathMod$choice.json"

        saveResultsToFile(finalPath , result)
        Toast.makeText(this,"Simulation Completed, Results are in Results-$choice.json",Toast.LENGTH_LONG).show()
    }

    private fun printPfLocations(particleFilter : ParticleFilterService, realPosition: Location) {
        Log.d("SIMULATION-PF", "TRILATERATION (${particleFilter.trilaterationLocation.x}, ${particleFilter.trilaterationLocation.y})")
        Log.d("SIMULATION-PF", "REAL POINT IS (${realPosition.x}, ${realPosition.y})")
        Log.d("SIMULATION-PF", "PF MIDDLE  IS (${particleFilter.pfLocation.x}, ${particleFilter.pfLocation.y})")
    }

    override fun displayMap() {// Loading the map from a JSON file
        super.displayMap()
        loadDatasetFromFile(datasetPath)
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
