package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import android.widget.ImageView
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonFingerprint
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonFingerprintZone
import ar.edu.unicen.exa.bconmanager.R

class FingerprintZone constructor (position : Location) : Resource(position){
    private val PIXELS_ERROR = 200
    private val red_image = R.drawable.finger_zone_red
    private val green_image = R.drawable.finger_zone_green
    private val blue_image = R.drawable.finger_zone_blue
    private var fingerprints = mutableListOf<Fingerprint>()
    var view : ImageView? = null
    var hasData : Boolean = false



    fun isTouched(x : Float, y: Float) : Boolean {
        // TO DO: Compare in meters instead of pixels
        if (hasData) {
            this.image = green_image
        } else {
            this.image = red_image
        }
        if ((Math.abs(x - position.getX()) <= PIXELS_ERROR) && (Math.abs(y - position.getY()) <= PIXELS_ERROR)) {
            return true
        }
        return false
    }

    fun touch()  {
        this.image = blue_image
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
        return "FingerprintZone: (${position.getX()} , ${position.getY()})"
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
}