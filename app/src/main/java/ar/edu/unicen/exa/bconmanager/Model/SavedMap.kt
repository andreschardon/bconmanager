package ar.edu.unicen.exa.bconmanager.Model

import java.lang.Math.abs

class SavedMap constructor (name : String, initialCoordinates : Pair<Double,Double>, endingCoordinates: Pair<Double,Double>) {
    var name = name
    var initialCoordinates : Pair<Double,Double> = initialCoordinates
    var endingCoordinates : Pair<Double,Double> = endingCoordinates

    fun isInRange(coordinates: Pair<Double,Double>) : Boolean {
        if ((abs(abs(coordinates.first) - abs(initialCoordinates.first)) < 0.0003) and
                (abs(abs(coordinates.second) - abs(initialCoordinates.second)) < 0.0003)) {
            return true
        }
        return false
    }
    override fun toString() : String {
        return "Name: $name, Initial Coords: ${initialCoordinates.first.toString()}, ${initialCoordinates.second.toString()}" /*+
                "Ending Coords: ${endingCoordinates.first}, ${endingCoordinates.second}"*/
    }
}