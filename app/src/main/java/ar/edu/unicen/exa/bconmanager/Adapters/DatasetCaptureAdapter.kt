package ar.edu.unicen.exa.bconmanager.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Controller.DatasetActivity
import ar.edu.unicen.exa.bconmanager.Controller.OnMapActivity
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice


class DatasetCaptureAdapter(context : DatasetActivity, beacons : List<BeaconDevice>) : BaseAdapter() {

    var context = context
    val beacons = beacons
    var firstTime = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val beaconView : View
        beaconView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
        return beaconView

    }

    fun stopRecordingAngle(){
        context.unsetStartingPoint()
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
        if(firstTime) {
            context.unsetStartingPoint()
            firstTime = false
        }
    }

}