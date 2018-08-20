package ar.edu.unicen.exa.bconmanager.Model

class Fingerprint constructor(var mac : String, var rssi : Double) {

    override fun toString() : String {
        return "Fingerprint for beacon $mac : $rssi"
    }
}