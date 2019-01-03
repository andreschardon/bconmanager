package ar.edu.unicen.exa.bconmanager.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Controller.OnMapActivity
import ar.edu.unicen.exa.bconmanager.Controller.PDRActivity
import ar.edu.unicen.exa.bconmanager.Controller.PDRInterface

class PDRAdapter(pdrContext: PDRInterface){

    var pdrContext = pdrContext


    fun StepDetected(){
        Log.d("PFACTIVITY", "Detected a step in the adapter")
            pdrContext.updatePosition()
    }
    fun stopRecordingAngle(){
        pdrContext.unsetStartingPoint()
    }
}