package ar.edu.unicen.exa.bconmanager.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.ParticleFilterActivity


class ParticleFilterAdapter(context : ParticleFilterActivity) : BaseAdapter() {

    var context = context
    var viewX: Double = 0.0
    var viewY: Double = 0.0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val beaconView : View
        beaconView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, null)
        return beaconView

    }

    override fun getItem(position: Int): Any {
        return 0.0;
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return 0;
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        context.updateParticleFilterPosition(viewX, viewY)
    }

}