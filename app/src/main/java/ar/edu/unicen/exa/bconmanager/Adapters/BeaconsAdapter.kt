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
        printBeaconsDistance()
    }

    private fun printBeaconsDistance() {
        beacons.forEach { Log.d("Adap", it.toString()) }
        context.trilateratePosition()
    }

}