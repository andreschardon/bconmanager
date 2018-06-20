package ar.edu.unicen.exa.bconmanager.Adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.FindMeActivity
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice

class BeaconsAdapter(context : FindMeActivity, beacons : List<BeaconDevice>) : BaseAdapter() {

    val context = context
    val beacons = beacons
    var counter = 0
    val REFRESH_RATE = 10
    val REFRESH_INTERMEDIATE = listOf(0, 2, 4, 6, 8)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val beaconView : View
        beaconView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
        return beaconView

    }

    override fun getItem(position: Int): Any {
        return beacons[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return beacons.count()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        //printBeaconsDistance()
        counter++
        if (counter == REFRESH_RATE) {
            Log.d("REFRESH", "NOW")
            counter = 0
            context.trilateratePosition()
        }
        if (counter in REFRESH_INTERMEDIATE) {
            context.updateIntermediate()
        }


    }

    /*fun notifyDataSetChanged(updateNow : Boolean) {
        if (updateNow)

    }*/

    private fun printBeaconsDistance() {
        beacons.forEach { Log.d("Adap", it.toString()) }
    }

}