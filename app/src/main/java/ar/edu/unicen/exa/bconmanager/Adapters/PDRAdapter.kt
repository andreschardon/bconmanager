package ar.edu.unicen.exa.bconmanager.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.PDRActivity

class PDRAdapter(context: PDRActivity) : BaseAdapter(){

    var context = context
    var firstTime = true

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val beaconView : View
        beaconView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
        return beaconView
    }
    override fun getItem(position: Int): Any {
        return 1
    }
    override fun getItemId(position: Int): Long {
        return 0
    }
    override fun getCount(): Int {
        return 0
        //return beacons.count()
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        if(firstTime) {
            context.unsetStartingPoint()
            firstTime = false
        }
        else {
            context.updatePosition()
        }
    }
}