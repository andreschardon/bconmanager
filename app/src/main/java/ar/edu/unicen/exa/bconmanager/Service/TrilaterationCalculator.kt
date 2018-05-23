package ar.edu.unicen.exa.bconmanager.Service

import android.graphics.Point
import android.support.v7.app.AppCompatActivity
import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap
import ar.edu.unicen.exa.bconmanager.Model.Circle
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import java.math.BigDecimal

class TrilaterationCalculator  : AppCompatActivity() {

    var mapHeight=0.0
    var mapWidth=0.0
    // Let EPS (epsilon) be a small value
    var EPS = 0.0000001;


    fun euclideanDistance(location1: Location, location2:Location):Double{
        var distance= 0.00

        distance= Math.sqrt(Math.pow(location1.x-location2.x,2.00) + Math.pow(location1.y-location2.y,2.00))
        return distance
    }
    fun getPositionInMap( map: CustomMap) {
        mapHeight=map.height
        mapWidth= map.width
        //val savedBeacons : List<BeaconOnMap> = map.savedBeacons
        //var beacon1: BeaconOnMap = savedBeacons.first()
        //var beacon2: BeaconOnMap = savedBeacons.last()

        /*for (locatedBeacon: BeaconOnMap in savedBeacons) {
            //val positions: List<Location> = getPositionsFromBeacon(locatedBeacon)
            //positions.forEach { println("Posicion X :${it.x}   Posicion Y: ${it.y}") }
        }
        */
        var circle1 = Circle(1.3,2.06,1.00)
        var circle2 = Circle(2.58,1.73,1.25)
        var intersectionLocations = mutableListOf<Location>()
        intersectionLocations = circleCircleIntersectionPoints(circle1,circle2) as MutableList<Location>
        val TAG = "INTERSECTION"
        intersectionLocations.forEach{Log.d(TAG,"INTERSECTION IN X : ${it.x}  INTERSECTION IN Y: ${it.y}")}
        var circle3 = Circle(3.1,1.2,0.4)
        var location3 = Location(circle3.x,circle3.y,map)
        if(euclideanDistance(intersectionLocations.first(),location3) == circle3.r){
            Log.d(TAG,"ES este")
        }
        else {
            Log.d(TAG,"ES el otro este")
        }
    }


// Due to double rounding precision the value passed into the Math.acos
// function may be outside its domain of [-1, +1] which would return
// the value NaN which we do not want.

    private fun acossafe(x: Double) : Double{
        if (x >= +1.0) {
            return 0.00
        }
        if (x <= -1.0) {
            return Math.PI
        }
        return Math.acos(x)
    }

// Rotates a point about a fixed point at some angle 'a'
    fun rotatePoint(fp: Location, pt: Location, a: Double) : Location{
        var x = pt.x - fp.x
        var y = pt.y - fp.y
        var xRot = x * Math.cos(a) + y * Math.sin(a)
        var yRot = y * Math.cos(a) - x * Math.sin(a)
        var rotatedPoint = Location(fp.x+xRot,fp.y+yRot,null)
        return rotatedPoint
    }

// Given two circles this method finds the intersection
// point(s) of the two circles (if any exists)
    fun circleCircleIntersectionPoints(c1: Circle, c2: Circle): List<Location>? {

        var r = 0.00
        var R = 0.00
        var d = 0.00
        var dx = 0.00
        var dy = 0.00
        var cx = 0.00
        var cy = 0.00
        var Cx = 0.00
        var Cy = 0.00
        var intersectPoints = mutableListOf<Location>()

        if (c1.r < c2.r) {
            r  = c1.r;  R = c2.r
            cx = c1.x; cy = c1.y
            Cx = c2.x; Cy = c2.y
        } else {
            r  = c2.r; R  = c1.r
            Cx = c1.x; Cy = c1.y
            cx = c2.x; cy = c2.y
        }

        // Compute the vector <dx, dy>
        dx = cx - Cx
        dy = cy - Cy

        // Find the distance between two points.
        d = Math.sqrt( dx*dx + dy*dy )

        // There are an infinite number of solutions
        // Seems appropriate to also return null
        if (d < EPS && Math.abs(R-r) < EPS) return null

        // No intersection (circles centered at the
        // same place with different size)
        else if (d < EPS) {
            return null
        }

        var x = (dx / d) * R + Cx
        var y = (dy / d) * R + Cy
        var P =  Location(x, y, null)

        // Single intersection (kissing circles)
        if (Math.abs((R+r)-d) < EPS || Math.abs(R-(r+d)) < EPS){
            intersectPoints.add(P)
            return intersectPoints
        }


        // No intersection. Either the small circle contained within
        // big circle or circles are simply disjoint.
        if ( (d+r) < R || (R+r < d) ) return null

        //Double intersection
        var C = Location(Cx, Cy,null)
        var angle = acossafe((r*r-d*d-R*R)/(-2.0*d*R))
        var pt1 = rotatePoint(C, P, +angle)
        var pt2 = rotatePoint(C, P, -angle)
        intersectPoints.add(pt1)
        intersectPoints.add(pt2)
        return intersectPoints

    }
}