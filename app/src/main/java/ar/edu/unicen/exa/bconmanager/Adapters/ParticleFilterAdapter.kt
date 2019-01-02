package ar.edu.unicen.exa.bconmanager.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.ParticleFilterActivity
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice


class ParticleFilterAdapter(context : ParticleFilterActivity, beacons : List<BeaconDevice>) : BaseAdapter() {

    var context = context
    val beacons = beacons
    var viewX: Double = 0.0
    var viewY: Double = 0.0

    var counter = 0
    var ref_counter = 0
    var cleanAverages = false
    val REFRESH_RATE = 10
    val CLEAN_RATE = 5
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

            Log.d("REFRESH", context.toString())
            counter = 0
            context.trilateratePosition()
            beacons.forEach {
                it.cleanAverages()
            }

        }
    }

    fun notifyStepDetected() {
        context.updateParticleFilterPosition(viewX, viewY)
    }

}