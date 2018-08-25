package ar.edu.unicen.exa.bconmanager.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.FingerprintOfflineActivity
import ar.edu.unicen.exa.bconmanager.Controller.FingerprintOnlineActivity
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice


class FingerprintOnlineAdapter(context : FingerprintOnlineActivity, beacons : List<BeaconDevice>) : BaseAdapter() {

    var context = context
    val beacons = beacons
    var counter = 0
    val REFRESH_RATE = 30

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
        counter++
        if (counter == REFRESH_RATE) {
            context.updatePosition(beacons)
            counter = 0
            beacons.forEach { it.cleanAverages() }
        }

    }

}