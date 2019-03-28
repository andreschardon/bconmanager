package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Model.AveragedTimestamp
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Json.*
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.*
import ar.edu.unicen.exa.bconmanager.Service.Storage.JsonUtility

class SimulationActivity : OnMapActivity() {

    private val datasetPathMod = "/storage/emulated/0/Download/results/"
    private var simulationData: MutableList<JsonData> = mutableListOf()
    private var pointsList: MutableList<ImageView> = mutableListOf()
    lateinit var algorithm: Algorithm

    private var drawPoints = true
    private var runAverageCalculation = false

    private val UPDATE_INTERVAL = 4
    private val GENERATE = false
    private val MAX_SIMULATIONS_AVERAGE = 30

    private var averageMaxError = 0.0
    private var averageErrorList: MutableList<Double> = mutableListOf()
    private var averageMeanErrorList: MutableList<Double> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simulation)
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        isChoosingFile = true
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)

    }

    override fun onResume() {
        isChoosingFile = false
        super.onResume()
        if (!datasetChosen) {
            val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
            val intent: Intent
            isChoosingFile = true
            chooseFile.type = "application/octet-stream" //as close to only Json as possible
            intent = Intent.createChooser(chooseFile, "Choose a dataset file")
            Toast.makeText(this, "Please select the DATASET and the MAP", Toast.LENGTH_SHORT).show()
            startActivityForResult(intent, 102)
        }

    }

    override fun onPause() {
        if (!isChoosingFile) {
            datasetChosen = false
        }
        super.onPause()
    }


    private fun loadDatasetFromFile(filePath: String) {
        val dataset = JsonUtility.readDatasetFromFile(filePath)
        for (d in dataset.data!!) {
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
        averageErrorList.clear()
        averageMeanErrorList.clear()
        averageMaxError = 0.0
        if (!runAverageCalculation) {
            algorithm = ParticleFilterService()
            (algorithm as ParticleFilterService).setUseFingerprinting(true)
            runSimulation("ParticleFilter")
        } else {
            var i = 0
            while (i < MAX_SIMULATIONS_AVERAGE) {
                Log.d("AVERROR", "RUNNING SIMULATION $i")
                algorithm = ParticleFilterService()
                (algorithm as ParticleFilterService).setUseFingerprinting(true)
                runSimulation("ParticleFilter")
                i++
            }
            printAveragedErrors()
        }


    }

    fun runSimulationPFDist(view: View) {
        averageErrorList.clear()
        averageMeanErrorList.clear()
        averageMaxError = 0.0
        if (!runAverageCalculation) {
            algorithm = ParticleFilterService()
            (algorithm as ParticleFilterService).setUseFingerprinting(false)
            runSimulation("ParticleFilterDist")
        } else {
            var i = 0
            while (i < MAX_SIMULATIONS_AVERAGE) {
                Log.d("AVERROR", "RUNNING SIMULATION $i")
                algorithm = ParticleFilterService()
                (algorithm as ParticleFilterService).setUseFingerprinting(false)
                runSimulation("ParticleFilterDist")
                i++
            }
            printAveragedErrors()
        }
    }

    private fun printAveragedErrors() {
        val averageError = averageErrorList.average()
        val meanError = averageMeanErrorList.average()

        Toast.makeText(this, "AV ERROR IS $averageError", Toast.LENGTH_LONG).show()

        Log.d("AVERROR", averageErrorList.toString())
        Log.d("AVERROR", "Average error is $averageError")
        Log.d("AVERROR", "MAX error is ${averageMaxError}")
        Log.d("AVERROR", averageMeanErrorList.toString())
        Log.d("AVERROR", "MEAN error is ${meanError}")
    }

    fun runSimulationPDR(view: View) {
        algorithm = PDRService.instance
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
        runSimulationPFDist(view)
    }

    private fun runSimulation(choice: String) {
        var maxError = 0.0
        var averageError = 0.0
        var result = JsonSimResult()
        var timestampList: MutableList<JsonTimestamp> = mutableListOf()
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
        val max = simulationDataSize - 15


        // We are going to run the simulation once every 3 "timestamps"
        var currentCounter = 0
        var totalCounter = 0
        var currentTimestamp = AveragedTimestamp()
        var simulationDataToDo: MutableList<JsonData> = mutableListOf()

        // TEMPORARY: TO GENERATE NEW DATASET
        if (GENERATE) {
            val dataset2 = JsonUtility.readDatasetFromFile("/storage/emulated/0/Download/rectangulo2.json")


            for (d in dataset2.data!!) {
                var data: JsonData = d
                simulationDataToDo.add(data)
            }
        }



        while (i < simulationDataSize) {
            val currentData = simulationData[i]

            // Check if it is the "last" timestamp
            var isLastTimestamp = false
            var nextTimestamp: Number = 0
            if ((i + 1) < simulationData!!.size) {
                nextTimestamp = simulationData!!.get(i + 1).timestamp
            } else {
                isLastTimestamp = true
                nextTimestamp = currentData.timestamp
            }
            //Log.d("AVERAGED", "Timestamp $i currentCounter $currentCounter")
            if (currentCounter == 0) {
                currentTimestamp.startFromData(currentData, nextTimestamp)
                totalCounter++
            } else {
                currentTimestamp.addData(currentData, nextTimestamp)

            }
            currentCounter++


            if (currentCounter == UPDATE_INTERVAL || isLastTimestamp) {
                Log.d("AVERAGED-f", "[$i] $currentTimestamp")
                if (GENERATE)
                    printNewRssi(currentTimestamp, totalCounter, simulationDataToDo)
                val calculatedPosition = algorithm.getNextPosition(currentTimestamp)
                calculatedPosition.x = calculatedPosition.x.roundTo2DecimalPlaces()
                calculatedPosition.y = calculatedPosition.y.roundTo2DecimalPlaces()
                Log.d("AVERAGED-f", "[$i] $calculatedPosition")
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

                val timestamp = JsonTimestamp(currentData.timestamp, currentData.positionX, currentData.positionY, error, calculatedPosition.x, calculatedPosition.y)
                timestampList.add(timestamp)

                if (algorithm is ParticleFilterService) {
                    printPfLocations(algorithm as ParticleFilterService, realPosition)
                }
                currentCounter = 0
            } else {
            }
            i++
        }
        errors.sort()
        averageError = errorSum / simulationData.size
        result.timestamps = timestampList
        result.errorMax = maxError
        result.errorAverage = averageError
        result.errorMedian = getMedianError(timestampList.size, errors)
        result.stdDev = calculateSD(errors)
        Log.d("ERROR", "ERROR MEDIAN: ${result.errorMedian}")
        Log.d("ERROR", "ERROR STDDEV: ${result.stdDev}")
        val finalPath = "$datasetPathMod$choice.json"

        if (!runAverageCalculation || !choice.contains("ParticleFilter")) {
            saveResultsToFile(finalPath, result)

            if (GENERATE) {
                val dataset3 = JsonDataset(simulationDataToDo)
                JsonUtility.saveDatasetToFile("/storage/emulated/0/Download/rectangulo-new.json", dataset3)
                Log.d("SIMULATION", "Finished saving to $filePath")
            }



            Toast.makeText(this, "ERROR IS ${result.errorAverage}", Toast.LENGTH_LONG).show()
            //Toast.makeText(this, "Simulation Completed, results are in Downloads/Results-$choice.json", Toast.LENGTH_LONG).show()
        } else {
            if (averageMaxError < maxError)
                averageMaxError = maxError
            averageErrorList.add(averageError)
            averageMeanErrorList.add(result.errorMedian!!)
        }

    }

    private fun printNewRssi(currentTimestamp: AveragedTimestamp, count: Int, simulationDataToDo: MutableList<JsonData>) {
        currentTimestamp.beacons.forEach {
            Log.d("NEWRSSI", "[$count] ${it.mac} ${it.rssi}")
            if (simulationDataToDo.size > count) {
                val beaconIndex = simulationDataToDo[count].beacons!!.indexOf(JsonDataBeacon(it.mac, it.rssi, null))
                if (beaconIndex != -1)
                    simulationDataToDo[count].beacons!![beaconIndex].rssi = it.rssi
            }

        }

    }

    private fun drawPosition(position: Location, realPosition: Boolean, index: Int, last: Int) {
        val particle = PositionOnMap(position)
        if (realPosition)
            particle.image = R.drawable.realposition
        else {
            Log.d("DRAWINGDEBUG", "Drawing result in $position")
            when {
                index <= 40 -> particle.image = R.drawable.finger_zone_green_xs
                index >= last -> particle.image = R.drawable.finger_zone_red_xs
                else -> particle.image = R.drawable.finger_zone_blue_xs
            }
        }

        val particleView = ImageView(this)
        this.pointsList.add(particleView)

        setupResource(particle, particleView, 20, 20)
    }

    private fun printPfLocations(particleFilter: ParticleFilterService, realPosition: Location) {
        if (particleFilter.referenceLocation != null)
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

    fun getMedianError(simSize: Int, errors: List<Double>): Double {
        for (e in errors) {
            Log.d("ERRORS", "E: $e")
        }
        val medianNOdd = ((simSize - 1) / 2)
        val medianNEven = (simSize / 2)
        if (simSize % 2 == 0) {
            Log.d("ERROR", "ERROR EVEN: ${(errors[medianNOdd] + errors[medianNEven]) / 2}")
            return (errors[medianNOdd] + errors[medianNEven]) / 2
        } else {
            Log.d("ERROR", "ERROR ODD: ${errors[medianNOdd]}")
            return errors[medianNOdd]
        }
    }

    fun calculateSD(errors: List<Double>): Double {
        var sum = 0.0
        var standardDeviation = 0.0

        for (num in errors) {
            sum += num
        }

        val mean = sum / errors.size

        for (num in errors) {
            standardDeviation += Math.pow(num - mean, 2.0)
        }

        val divider = errors.size - 1

        return Math.sqrt(standardDeviation / divider )
    }
}
