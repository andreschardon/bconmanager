package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.support.v7.app.AppCompatActivity
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location

abstract class Algorithm : AppCompatActivity() {

    protected open var TAG : String = ""
    protected lateinit var customMap : CustomMap

    fun startUp(map: CustomMap) {
        customMap = map
    }

    abstract fun getNextPosition(data: JsonData, nextTimestamp: Number): Location
    protected fun getBeacons(data: JsonData) : List<BeaconDevice> {
        var savedBeacons: MutableList<BeaconDevice> = mutableListOf<BeaconDevice>()
        for (beacon in data.beacons!!) {
            savedBeacons.add(BeaconDevice(beacon.mac!!, beacon.rssi!!, null))
        }
        return savedBeacons
    }
}