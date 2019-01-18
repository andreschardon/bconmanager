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
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.*
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import kotlinx.android.synthetic.main.activity_simulation.*

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
        pdrBtn.isClickable = false
        trilaterationBtn.isClickable = false
        fingerPrintingBtn.isClickable = false
        runSimulation(1)
        pdrBtn.isClickable = true
        trilaterationBtn.isClickable = true
        fingerPrintingBtn.isClickable = true
    }
    fun runSimulationPDR(view : View) {
        algorithm =  PDRService.instance
        (algorithm as PDRService).setAdjustedBearing(floorMap.angle.toFloat())
        particleFilterBtn.isClickable = false
        trilaterationBtn.isClickable = false
        fingerPrintingBtn.isClickable = false
        runSimulation(1)
        particleFilterBtn.isClickable = true
        trilaterationBtn.isClickable = true
        fingerPrintingBtn.isClickable = true
    }

    fun runSimulationTrilat(view: View) {
        algorithm = TrilaterationService.instance
        pdrBtn.isClickable = false
        particleFilterBtn.isClickable = false
        fingerPrintingBtn.isClickable = false
        runSimulation(1)
        pdrBtn.isClickable = true
        particleFilterBtn.isClickable = true
        fingerPrintingBtn.isClickable = true
    }

    fun runSimulationFingerprinting(view: View) {
        algorithm = FingerprintingService()
        pdrBtn.isClickable = false
        particleFilterBtn.isClickable = false
        trilaterationBtn.isClickable = false
        runSimulation(1)
        pdrBtn.isClickable = true
        particleFilterBtn.isClickable = true
        trilaterationBtn.isClickable = true
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
            Log.d("SIMULATION-f", "[$i] " + calculatedPosition.toString())
            var realPosition = Location(currentData.positionX,currentData.positionY,floorMap)
            currentData.error = algorithm.getError(realPosition,calculatedPosition)
            currentData.estimateX = calculatedPosition.x
            currentData.estimateY = calculatedPosition.y
            currentData.beacons = null
            if (algorithm is ParticleFilterService) {
                printPfLocations(algorithm as ParticleFilterService, realPosition)
            }
            i++
        }
        saveDatasetToFile(datasetPathMod)
        Toast.makeText(this,"Simulation Completed, Results are in Dataset2.json",Toast.LENGTH_LONG).show()
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
