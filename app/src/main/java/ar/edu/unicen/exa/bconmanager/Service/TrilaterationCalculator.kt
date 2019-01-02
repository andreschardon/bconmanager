package ar.edu.unicen.exa.bconmanager.Service

import android.support.v7.app.AppCompatActivity
import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.Circle
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Location

class TrilaterationCalculator : Algorithm() {

    private var mapHeight = 0.0
    private var mapWidth = 0.0
    // Let EPS (epsilon) be a small value
    private var EPS = 0.0000001
    override var  TAG = "Trilateration Calculator"
    private val maxLength = 50.0

    private object Holder {
        val INSTANCE = TrilaterationCalculator()
    }

    companion object {
        val instance: TrilaterationCalculator by lazy { Holder.INSTANCE }
    }


    /**
     * Returns the current location on the map based on
     * the distance to all the beacons
     */
    //REPLACE WITH getNextPosition()
    fun getPositionInMap(map: CustomMap): Location? {
        mapHeight = map.height
                mapWidth = map.width

        /** Calculate the three closest circles **/
        Log.d("SAVED", "${map.savedBeacons}")
        val sortedList = map.sortBeaconsByDistance()

        val beacon0 = sortedList[0]
        val beacon1 = sortedList[1]
        val beacon2 = sortedList[2]
        val beacon3 = sortedList[3]

        Log.d("CLOSEST",
                "1: ${beacon0.beacon.name} at ${beacon0.beacon.approxDistance}mts // " +
                        "2: ${beacon1.beacon.name} at ${beacon1.beacon.approxDistance}mts // " +
                        "3: ${beacon2.beacon.name} at ${beacon2.beacon.approxDistance}mts" +
                        "3: ${beacon3.beacon.name} at ${beacon3.beacon.approxDistance}mts")

        var circle0 = Circle.fromBeacon(beacon0)
        var circle1 = Circle.fromBeacon(beacon1)
        var circle2 = Circle.fromBeacon(beacon2)

        /** Let's calculate the intersections between all those circles **/
        Log.d(TAG, circle0.toString())
        Log.d(TAG, circle1.toString())
        Log.d(TAG, circle2.toString())

        var intersectionLocations = mutableListOf<Location>()
        var intersection01 = circleCircleIntersectionPoints(circle0, circle1)
        var intersection02 = circleCircleIntersectionPoints(circle0, circle2)
        var intersection12 = circleCircleIntersectionPoints(circle1, circle2)

        if (intersection01 == null && intersection02 == null && intersection12 == null) {
            // Either we are too far away of all three beacons
            // Or we are in the middle and we should increase the radius a little
            var counter = 0
            while (counter < 30) {

                if ((circle1.r < maxLength) && (circle2.r < maxLength) && (circle0.r < maxLength)) {
                    // We should consider some error margin
                    //Log.d(TAG, "Increasing beacon's distance by 0.1")
                    circle0 = increaseCircleRadius(circle0)
                    circle1 = increaseCircleRadius(circle1)
                    circle2 = increaseCircleRadius(circle2)
                    Log.d(TAG, circle0.toString())
                    Log.d(TAG, circle1.toString())
                    Log.d(TAG, circle2.toString())
                } else {
                    // We are too far away
                    //Log.d(TAG, "We are too far away from at least one beacon")
                    return null
                }

                intersection01 = circleCircleIntersectionPoints(circle0, circle1)
                intersection02 = circleCircleIntersectionPoints(circle0, circle2)
                intersection12 = circleCircleIntersectionPoints(circle1, circle2)

                if (intersection01 == null && intersection02 == null && intersection12 == null) {
                    //Log.d(TAG, "No intersections yet, try again")
                    counter++
                } else {
                    //Log.d(TAG, "New intersection found! Continue")
                    counter = 31
                }

            }
            if (counter == 30) {
                // We couldn't fix it by increasing the radius
                //Log.d(TAG, " :( We reached the maximum attempts")
                return null
            }

        }


        var location3: Location? = null
        var furthestCircle: Circle? = null

        var continueForcing = true
        while (continueForcing) {
            if (intersection01 != null) {
                //Log.d(TAG, "Intersection01")
                intersectionLocations = intersection01 as MutableList<Location>
                location3 = Location(circle2.x, circle2.y, map)
                furthestCircle = circle2
                continueForcing = false
            } else if (intersection02 != null) {
                //Log.d(TAG, "Intersection02")
                intersectionLocations = intersection02 as MutableList<Location>
                location3 = Location(circle1.x, circle1.y, map)
                furthestCircle = circle1
                continueForcing = false
            } else if (intersection12 != null &&
                    (circle2.r < maxLength) &&
                    (circle1.r < maxLength)) {
                // This is not very precise, we should force an intersection between 0 and 1 here
                /*Log.d(TAG, "Intersection12")
                intersectionLocations =  intersection12 as MutableList<Location>
                location3 = Location(circle0.x,circle0.y,map)
                furthestCircle = circle0*/
                // Force an intersection between 0 and 1
                //Log.d(TAG, "Error ${circle1.r + 0.5}")
                circle1 = Circle(beacon1.position.x, beacon1.position.y, circle1.r + 0.5)
                intersection01 = circleCircleIntersectionPoints(circle0, circle1)
                if (intersection01 == null) {
                    //Log.d(TAG, "It is still null")
                }
            } else {
                //Log.d(TAG, "There are no intersections")
                continueForcing = false
                return null
            }
        }

        //intersectionLocations.forEach{Log.d("CLOSEST","INTERSECTION IN X : ${it.x}  INTERSECTION IN Y: ${it.y}")}

        // We have the distance between our position and the furthest beacon
        // We need to calculate the distance between the two points and that beacon and choose the
        // one that is the closest to the "real" distance
        //Log.d("DISTANCE", "Real distance is ${furthestCircle!!.r}")
        //Log.d("DISTANCE", "First distance is ${euclideanDistance(intersectionLocations[0], location3!!)}")

        if (intersectionLocations.size == 1) {
            //Log.d("CRASH_FIX", "No second intersection, duplicate the first one")
            intersectionLocations.add(intersectionLocations[0])
        }

        //Log.d("DISTANCE", "Second distance is ${euclideanDistance(intersectionLocations[1], location3!!)}")
        val firstDistance = Math.abs((furthestCircle!!.r - (euclideanDistance(intersectionLocations[0], location3!!))))
        val secondDistance = Math.abs((furthestCircle!!.r - (euclideanDistance(intersectionLocations[1], location3!!))))

        if ((firstDistance <= secondDistance)) {
            return forceInsideMap(intersectionLocations[0]!!)
        } else {
            return forceInsideMap(intersectionLocations[1]!!)
        }

    }

    /**
     *  Increase a circle's radius 0.5 mts (to consider errors)
     */
    private fun increaseCircleRadius(circle: Circle): Circle {
        return Circle(circle.x, circle.y, circle.r + 0.1)
    }

    /**
     * Restrain the position to the map
     */
    private fun forceInsideMap(location: Location): Location {
        if (location.x < 0.0) {
            location.x = 0.0
        } else if (location.x > mapWidth) {
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

    private fun acossafe(x: Double): Double {
        if (x >= +1.0) {
            return 0.00
        }
        if (x <= -1.0) {
            return Math.PI
        }
        return Math.acos(x)
    }

    // Rotates a point about a fixed point at some angle 'a'
    fun rotatePoint(fp: Location, pt: Location, a: Double): Location {
        val x = pt.x - fp.x
        val y = pt.y - fp.y
        val xRot = x * Math.cos(a) + y * Math.sin(a)
        val yRot = y * Math.cos(a) - x * Math.sin(a)
        return Location(fp.x + xRot, fp.y + yRot, null)
    }

    // Given two circles this method finds the intersection
    // point(s) of the two circles (if any exists)
    private fun circleCircleIntersectionPoints(c1: Circle, c2: Circle): List<Location>? {

        var r = 0.00
        var R = 0.00
        var d = 0.00
        var dx = 0.00
        var dy = 0.00
        var cx = 0.00
        var cy = 0.00
        var Cx = 0.00
        var Cy = 0.00
        val intersectPoints = mutableListOf<Location>()

        if (c1.r < c2.r) {
            r = c1.r; R = c2.r
            cx = c1.x; cy = c1.y
            Cx = c2.x; Cy = c2.y
        } else {
            r = c2.r; R = c1.r
            Cx = c1.x; Cy = c1.y
            cx = c2.x; cy = c2.y
        }

        // Compute the vector <dx, dy>
        dx = cx - Cx
        dy = cy - Cy

        // Find the distance between two points.
        d = Math.sqrt(dx * dx + dy * dy)

        // There are an infinite number of solutions
        // Seems appropriate to also return null
        if (d < EPS && Math.abs(R - r) < EPS) return null

        // No intersection (circles centered at the
        // same place with different size)
        else if (d < EPS) {
            return null
        }

        val x = (dx / d) * R + Cx
        val y = (dy / d) * R + Cy
        val P = Location(x, y, null)

        // Single intersection (kissing circles)
        if (Math.abs((R + r) - d) < EPS || Math.abs(R - (r + d)) < EPS) {
            intersectPoints.add(P)
            return intersectPoints
        }


        // No intersection. Either the small circle contained within
        // big circle or circles are simply disjoint.
        if ((d + r) < R || (R + r < d)) return null

        //Double intersection
        val C = Location(Cx, Cy, null)
        val angle = acossafe((r * r - d * d - R * R) / (-2.0 * d * R))
        val pt1 = rotatePoint(C, P, +angle)
        val pt2 = rotatePoint(C, P, -angle)
        intersectPoints.add(pt1)
        intersectPoints.add(pt2)
        return intersectPoints

    }

    fun euclideanDistance(location1: Location, location2: Location): Double {
        var distance = 0.00
        distance = Math.sqrt(Math.pow(location1.x - location2.x, 2.00) + Math.pow(location1.y - location2.y, 2.00))
        return distance
    }

    override fun getNextPosition(): Location {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}