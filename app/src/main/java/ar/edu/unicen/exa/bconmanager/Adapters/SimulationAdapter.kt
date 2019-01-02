package ar.edu.unicen.exa.bconmanager.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.SimulationActivity

class SimulationAdapter(context: SimulationActivity) : BaseAdapter(){

    var context = context
    var counter = 0
    private val REFRESH_RATE = 30

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
        counter++
        if (counter == REFRESH_RATE) {
            //context.updatePosition(beacons)
            counter = 0
            //beacons.forEach { it.cleanAverages() }
        }

    }
}