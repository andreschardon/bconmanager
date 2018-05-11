package ar.edu.unicen.exa.bconmanager.Model

import android.graphics.drawable.Drawable

class BeaconOnMap constructor (position : Location, beacon : BeaconDevice){
    var position : Location = position // x, y
    var beacon : BeaconDevice = beacon
    var reach : Double? = null // range (3m? 4m?)
    var image : Int? = null


}