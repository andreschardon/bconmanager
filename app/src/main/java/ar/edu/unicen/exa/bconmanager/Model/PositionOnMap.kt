package ar.edu.unicen.exa.bconmanager.Model

class PositionOnMap constructor (position : Location) : Resource(position){

    override fun toString(): String {
        return "PositionOnMap: (${position.x} , ${position.y})"
    }
}