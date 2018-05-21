package ar.edu.unicen.exa.bconmanager.Model

class BeaconOnMap constructor (position : Location, beacon : BeaconDevice){
    var position : Location = position // x, y
    var beacon : BeaconDevice = beacon
    var reach : Double? = null // range (3m? 4m?)
    var image : Int? = null

    fun toJson() : JsonBeacon {
        val beac = JsonBeacon(beacon.address, position.x, position.y)
        return beac
    }
}