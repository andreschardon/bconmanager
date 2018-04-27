package ar.edu.unicen.exa.bconmanager.Model

class BeaconOnMap constructor (position : Location, beacon : BeaconDevice){
    var position : Location = position // x, y
    var beacon : BeaconDevice = beacon
    var reach : Double? = null // range (3m? 4m?)
}