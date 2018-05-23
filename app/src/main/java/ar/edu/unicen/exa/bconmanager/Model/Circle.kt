package ar.edu.unicen.exa.bconmanager.Model

class Circle constructor(var x : Double, var y : Double, val r : Double) {

    override fun toString() : String {
        return "Position is ($x, $y) distance $r"
    }
}

