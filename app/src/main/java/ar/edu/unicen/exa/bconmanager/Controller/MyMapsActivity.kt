package ar.edu.unicen.exa.bconmanager.Controller

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import ar.edu.unicen.exa.bconmanager.Model.SavedMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_my_beacons.*
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent.getActivity
import android.content.DialogInterface
import android.R.string.cancel
import android.R.string.ok




class MyMapsActivity : AppCompatActivity() {

    private var savedMaps = mutableListOf<SavedMap>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var relativePath = "/storage/emulated/0/Download/"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_maps)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val path = "/storage/emulated/0/Download/savedMaps.json" //change for filepath - previous file + 'savedMaps.json'
        getSavedMaps(path)
        obtieneLocalizacion()
    }


    private fun goToFindMeActivity( name : String) {
        val intent = Intent(this, FindMeActivity::class.java)
        intent.putExtra("path",relativePath+name)
        startActivity(intent)
    }

    fun showDialog(name : String){
        val builder = AlertDialog.Builder(this)

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage("Do you want to see the map?")
                .setTitle("Map found!")
        builder.setPositiveButton("Go!", DialogInterface.OnClickListener { dialog, id ->
            goToFindMeActivity(name)
        })
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, id ->
            // User cancelled the dialog
        })

        val dialog = builder.create()
        dialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun obtieneLocalizacion(){
                    fusedLocationClient.lastLocation
                            .addOnSuccessListener { location: android.location.Location? ->
                    var latitude = location?.latitude
                    var longitude = location?.longitude
                    val coords = Pair(latitude as Double,longitude as Double)
                    Log.d("GPS", "LATITUDE IS : $latitude LONGITUDE IS : $longitude")
                    savedMaps.forEach { map->
                        if(map.isInRange(coords)){
                            Log.d("SAVEDMAPS","MAP IN RANGE DETECTED")
                            showDialog(map.name)
                        }
                        else {
                            Log.d("SAVEDMAPS","MAP OUT OF RANGE")
                            //showDialog(map.name)
                        }
                    }
                }
    }

    private fun getSavedMaps(filePath: String) {

        val jsonSavedMaps = JsonUtility.getSavedMaps(filePath)
        for (map in jsonSavedMaps.maps!!) {

            val map = SavedMap(map.name, Pair(map.initialCoordinates.latitude,map.initialCoordinates.longitude), Pair(map.endingCoordinates.latitude,map.endingCoordinates.longitude))
            savedMaps.add(map)
        }
    }

}