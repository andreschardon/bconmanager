package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import android.widget.ImageView
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonFingerprint
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonFingerprintZone
import ar.edu.unicen.exa.bconmanager.R

class FingerprintZone constructor (position : Location) : Resource(position){
    private val MTS_ERROR = 1.5
    private val red_image = R.drawable.finger_zone_red_xs
    private val green_image = R.drawable.finger_zone_green_xs
    private val blue_image = R.drawable.finger_zone_blue_xs
    private val radius = 1.5
    var fingerprints = mutableListOf<Fingerprint>()
    var view : ImageView? = null
    var hasData : Boolean = false
    var shouldRedraw : Boolean = false



    fun isTouched(x : Double, y: Double) : Boolean {
        val scaled = Location(0.0, 0.0, position.map)
        scaled.setX(x.toInt())
        scaled.setY(y.toInt())
        if (hasData) {
            this.image = green_image
        } else {
            this.image = red_image
        }
        if ((Math.abs(scaled.x - position.x) <= MTS_ERROR) && (Math.abs(scaled.y - position.y) <= MTS_ERROR)) {
            return true
        }
        return false
    }

    fun touch()  {
        this.image = blue_image
        this.shouldRedraw = true
    }

    fun unTouch() {
        if (hasData) {
            this.image = green_image
        } else {
            this.image = red_image
        }
        this.shouldRedraw = true
    }


    fun toJson() : JsonFingerprintZone {
        var list = mutableListOf<JsonFingerprint>()
        fingerprints.forEach {
            val fp = JsonFingerprint(it.mac, it.rssi)
            list.add(fp)
        }
        val fingerprintZone = JsonFingerprintZone(position.x, position.y, list)
        return fingerprintZone
    }

    override fun toString(): String {
        return "(${position.getXMeters()} , ${position.getYMeters()})"
    }

    fun updateFingerprints(beacons: List<BeaconDevice>) {
        if (hasData) {
            fingerprints.clear()
        }
        hasData = true
        this.image = green_image
        beacons.forEach {
            val fp = Fingerprint(it.address, it.averageRssi)
            fingerprints.add(fp)
        }
        Log.d("FPZONE","FingerprintZone $fingerprints")
    }

    fun getRadius(): Double {
        return this.radius
    }
}