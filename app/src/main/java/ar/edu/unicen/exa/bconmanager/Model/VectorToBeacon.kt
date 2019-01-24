package ar.edu.unicen.exa.bconmanager.Model

class VectorToBeacon constructor (address: String, distance: Double, angle: Double) {
    var distance: Double = distance
    var id: String = address
    var angle: Double = angle

    override fun toString(): String {
        return "$id : $distance mts at $angle°"
    }
}