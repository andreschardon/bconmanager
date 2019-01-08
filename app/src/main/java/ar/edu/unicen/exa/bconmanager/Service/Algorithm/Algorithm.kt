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


    /**
     * Converts jsonData beacons to BeaconOnMap list
     */
    protected fun getBeacons(data: JsonData) : List<BeaconOnMap> {
        var savedBeacons: MutableList<BeaconOnMap> = mutableListOf<BeaconOnMap>()
        for (beacon in data.beacons!!) {
            var beaconDev = BeaconDevice(beacon.mac!!, beacon.rssi!!, null)
            var beaconLoc = Location(0.0, 0.0, customMap)

            for (it in customMap.savedBeacons) {
                if (it.beacon == beaconDev) {
                    beaconLoc = it.position
                }
            }
            var beaconMap = BeaconOnMap(beaconLoc, beaconDev)
            savedBeacons.add(beaconMap)
        }
        return savedBeacons
    }
}