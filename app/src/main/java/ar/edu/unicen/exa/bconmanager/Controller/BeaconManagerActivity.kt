package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import ar.edu.unicen.exa.bconmanager.R

class BeaconManagerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon_manager)
    }

    fun goToMyBeaconsActivity(view : View) {
        val intent = Intent(this, MyBeaconsActivity::class.java)
        startActivity(intent)
    }

    fun goToFindMeActivity(view : View) {
        val intent = Intent(this, FindMeActivity::class.java)
        startActivity(intent)
    }
}
