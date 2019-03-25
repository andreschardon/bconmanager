package ar.edu.unicen.exa.bconmanager.Controller

import ar.edu.unicen.exa.bconmanager.Service.Algorithm.PDRService

interface PDRInterface {
    var pdrService : PDRService

    /**
     * Gets called whenever a new step is detected
     */
    fun updatePosition()


    /**
     * When..?
     */
    fun unsetStartingPoint()

}