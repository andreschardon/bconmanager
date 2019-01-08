package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.support.v7.app.AppCompatActivity
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location

abstract class Algorithm : AppCompatActivity() {

    protected open var TAG : String = ""
    protected lateinit var customMap : CustomMap

    fun startUp(map: CustomMap) {
        customMap = map
    }

    abstract fun getNextPosition(data: JsonData, nextTimestamp: Number): Location

}