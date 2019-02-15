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
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.*
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import kotlinx.android.synthetic.main.activity_simulation.*

class SimulationActivity : OnMapActivity() {

    private val datasetPath = "/storage/emulated/0/Download/zigzag3.json"
    private val datasetPathMod = "/storage/emulated/0/Download/results/"
    private var simulationData: MutableList<JsonData> = mutableListOf()
    private var pointsList: MutableList<ImageView> = mutableListOf()
    lateinit var algorithm: Algorithm

    lateinit var algorithmFingerp : FingerprintingService
    lateinit var algorithmTrilat: TrilaterationService
    lateinit var algorithmPDR: PDRService
    lateinit var algorithmPF: ParticleFilterService

    private var drawPoints = true


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
        //(algorithm as PDRService).setAdjustedBearing(0.0f)
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
        runSimulationTrilat(view)
        runSimulationPDR(view)
        runSimulationFingerprinting(view)
        runFPTrilatSimulation(view)
        runSimulationPF(view)
    }

    private fun runSimulation(choice: String) {
        var maxError = 0.0
        var averageError = 0.0
        var result = JsonSimResult()
        var timestampList : MutableList<JsonTimestamp> = mutableListOf()
        pointsList.forEach {
            removeResource(it)
        }
        pointsList.clear()

        algorithm.startUp(floorMap)
        var i = 0
        //Log.d("SIMULATION", "Size is ${simulationData!!.size}")
        var errorSum = 0.0
        val simulationDataSize = simulationData.size
        var errors: MutableList<Double> = mutableListOf()
        val max = simulationDataSize - 5
        while (i < simulationDataSize) {
            val currentData = simulationData[i]
            //Log.d("SIMULATION", currentData.toString())
            var nextTimestamp: Number = 0
            if ((i + 1) < simulationData!!.size) {
                nextTimestamp = simulationData!!.get(i + 1).timestamp
            } else
                nextTimestamp = currentData.timestamp
            val calculatedPosition = algorithm.getNextPosition(currentData, nextTimestamp)
            calculatedPosition.x = calculatedPosition.x.roundTo2DecimalPlaces()
            calculatedPosition.y = calculatedPosition.y.roundTo2DecimalPlaces()
            Log.d("SIMULATION-f", "[$i] " + calculatedPosition.toString())
            val realPosition = Location(currentData.positionX, currentData.positionY, floorMap)
            if (drawPoints) {
                drawPosition(calculatedPosition, false, i, max)
                drawPosition(realPosition, true, i, max)
            }

            // Calculate error
            val error = algorithm.getError(realPosition, calculatedPosition)
            errorSum += error
            errors.add(error)
            if (error >= maxError)
                maxError = error

            var timestamp = JsonTimestamp(currentData.timestamp, currentData.positionX, currentData.positionY, error, calculatedPosition.x, calculatedPosition.y)
            timestampList.add(timestamp)

            if (algorithm is ParticleFilterService) {
                printPfLocations(algorithm as ParticleFilterService, realPosition)
            }
            i++
        }
        errors.sort()
        averageError = errorSum / simulationData.size
        result.timestamps = timestampList
        result.errorMax = maxError
        result.errorAverage = averageError
        result.errorMedian = getMedianError(simulationDataSize,errors)
        Log.d("ERROR","ERROR MEDIAN: ${result.errorMedian}")
        val finalPath = "$datasetPathMod$choice.json"

        saveResultsToFile(finalPath , result)
        Toast.makeText(this,"Simulation Completed, Results are in Results-$choice.json",Toast.LENGTH_LONG).show()
    }

    private fun drawPosition(position: Location, realPosition: Boolean, index : Int, last : Int) {
        val particle = PositionOnMap(position)
        if (realPosition)
            particle.image = R.drawable.realposition
        else {
            Log.d("DRAWINGDEBUG", "Drawing result in $position")
            if (index <= 5) {
                particle.image = R.drawable.finger_zone_green_xs
            } else if (index >= last) {
                particle.image = R.drawable.finger_zone_red_xs
            }
            else
                particle.image = R.drawable.finger_zone_blue_xs
        }

        val particleView = ImageView(this)
        this.pointsList.add(particleView)

        setupResource(particle, particleView,20,20)
    }

    private fun printPfLocations(particleFilter : ParticleFilterService, realPosition: Location) {
        Log.d("SIMULATION-PF", "TRILATERATION (${particleFilter.referenceLocation.x}, ${particleFilter.referenceLocation.y})")
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

    fun getMedianError(simSize: Int, errors: List<Double>) : Double {
        for(e in errors) {
            Log.d("ERRORS", "E: $e")
        }
        val medianNOdd = ((simSize - 1)/2)
        val medianNEven = (simSize/2)
        if (simSize % 2 == 0) {
            Log.d("ERROR","ERROR EVEN: ${(errors[medianNOdd] + errors[medianNEven])/2}")
            return (errors[medianNOdd] + errors[medianNEven])/2
        }
        else {
            Log.d("ERROR", "ERROR ODD: ${errors[medianNOdd]}")
           return errors[medianNOdd]
        }
    }
}
