package ar.edu.unicen.exa.bconmanager.Model

import ar.edu.unicen.exa.bconmanager.Model.Json.JsonBeacon

class BeaconOnMap constructor (position : Location, beacon : BeaconDevice) : Resource(position){
    var beacon : BeaconDevice = beacon
    var reach : Double? = null // range (3m? 4m?)

    fun toJson() : JsonBeacon {
        val beac = JsonBeacon(beacon.address, beacon.name, position.x, position.y)
        return beac
    }

    override fun toString(): String {
        return "BeaconOnMap: (${position.x} , ${position.y}) mac: ${beacon.address}"
    }

    fun toStringDistance() : String {
        return "${beacon.name}: ${beacon.approxDistance}"
    }
}