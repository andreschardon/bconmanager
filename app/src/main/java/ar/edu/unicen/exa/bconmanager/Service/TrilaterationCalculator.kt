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
    fun getPositionInMap( map: CustomMap) : Location? {
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

        val sortedList = map.savedBeacons.sortedWith(compareBy({ it.beacon.approxDistance }))

        val beacon0 = sortedList[0]
        val beacon1 = sortedList[1]
        val beacon2 = sortedList[2]

        Log.d("CLOSEST", "1: ${beacon0.beacon.name} at ${beacon0.beacon.approxDistance}mts // 2: ${beacon1.beacon.name} at ${beacon1.beacon.approxDistance}mts // 3: ${beacon2.beacon.name} at ${beacon2.beacon.approxDistance}mts")


        var circle0 = Circle(beacon0.position.x, beacon0.position.y, beacon0.beacon.approxDistance)
        var circle1 = Circle(beacon1.position.x, beacon1.position.y, beacon1.beacon.approxDistance)
        var circle2 = Circle(beacon2.position.x, beacon2.position.y, beacon2.beacon.approxDistance)

        val TAG = "INTERSECTION"
        Log.d(TAG, circle0.toString())
        Log.d(TAG, circle1.toString())
        Log.d(TAG, circle2.toString())


        var intersectionLocations = mutableListOf<Location>()
        var intersection01 = circleCircleIntersectionPoints(circle0,circle1)
        val intersection02 = circleCircleIntersectionPoints(circle0,circle2)
        val intersection12 = circleCircleIntersectionPoints(circle1,circle2)
        var location3 : Location? = null
        var furthestCircle : Circle? = null

        var continueForcing = true
        while (continueForcing) {
            if (intersection01 != null) {
                Log.d(TAG, "Intersection01")
                intersectionLocations = intersection01 as MutableList<Location>
                location3 = Location(circle2.x, circle2.y, map)
                furthestCircle = circle2
                continueForcing = false
            } else if (intersection02 != null) {
                Log.d(TAG, "Intersection02")
                intersectionLocations = intersection02 as MutableList<Location>
                location3 = Location(circle1.x, circle1.y, map)
                furthestCircle = circle1
                continueForcing = false
            } else if (intersection12 != null && (circle1.r != 999.0)) {
            // This is not very precise, we should force an intersection between 0 and 1 here
                /*Log.d(TAG, "Intersection12")
                intersectionLocations =  intersection12 as MutableList<Location>
                location3 = Location(circle0.x,circle0.y,map)
                furthestCircle = circle0*/
                // Force an intersection between 0 and 1
                Log.d(TAG, "Kernel panic ${circle1.r + 0.1}")
                circle1 = Circle(beacon1.position.x, beacon1.position.y, circle1.r + 0.1)
                intersection01 = circleCircleIntersectionPoints(circle0,circle1)
                if (intersection01 == null) {
                    Log.d(TAG, "It is still null")
                }
            } else {
                continueForcing = false
                return null
            }
        }

        //intersectionLocations.forEach{Log.d("CLOSEST","INTERSECTION IN X : ${it.x}  INTERSECTION IN Y: ${it.y}")}

        // We have the distance between our position and the furthest beacon
        // We need to calculate the distance between the two points and that beacon and choose the
        // one that is the closest to the "real" distance
        Log.d("DISTANCE", "Real distance is ${furthestCircle!!.r}")
        Log.d("DISTANCE", "First distance is ${euclideanDistance(intersectionLocations[0], location3!!)}")
        Log.d("DISTANCE", "Second distance is ${euclideanDistance(intersectionLocations[1], location3!!)}")
        val firstDistance = Math.abs((furthestCircle!!.r - (euclideanDistance(intersectionLocations[0], location3!!))))
        val secondDistance = Math.abs((furthestCircle!!.r - (euclideanDistance(intersectionLocations[1], location3!!))))

        if ((firstDistance <= secondDistance)) {
            Log.d(TAG,"ES este")
            return forceInsideMap(intersectionLocations[0]!!)
        } else {
            Log.d(TAG,"ES el otro este")
            return forceInsideMap(intersectionLocations[1]!!)
        }

    }

    /**
     * Restrain the position to the map
     */
    private fun forceInsideMap(location : Location) : Location {
        if (location.x < 0.0) {
            location.x = 0.0
        } else if (location.x > mapWidth){
            location.x = mapWidth
        }
        if (location.y < 0.0) {
            location.y = 0.0
        } else if (location.y > mapHeight) {
            location.y = mapHeight
        }
        //Log.d("CLOSEST", "Final position is (${location.x}, ${location.y})")
        return location
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