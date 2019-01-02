package ar.edu.unicen.exa.bconmanager.Service

import android.support.v7.app.AppCompatActivity
import ar.edu.unicen.exa.bconmanager.Model.Location

abstract class Algorithm : AppCompatActivity() {

    protected open var TAG : String = ""
    abstract fun getNextPosition(): Location

}