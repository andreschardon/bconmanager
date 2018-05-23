package ar.edu.unicen.exa.bconmanager.Model

class PositionOnMap constructor (position : Location){
    var position : Location = position // x, y
    var image : Int? = null

    override fun toString(): String {
        return "PositionOnMap: (${position.x} , ${position.y})"
    }
}