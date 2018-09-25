package ar.edu.unicen.exa.bconmanager.Service;

import android.util.Log;
import ar.edu.unicen.exa.bconmanager.Model.Location;


public class StepPositioningHandler {

    private Location mCurrentLocation;
    private String TAG = "StepPositioningHandler";
    public Location getmCurrentLocation() {
        return mCurrentLocation;
    }

    public void setmCurrentLocation(Location mCurrentLocation) {
        Log.d(TAG,"CURRENT LOCATION IS : "+ mCurrentLocation.toString());
        this.mCurrentLocation = mCurrentLocation;
    }

    /** Calculates the new user position from the current one
     * @param stepSize the size of the step the user has made
     * @param bearing the angle of direction
     * @return new location
     */
    public Location computeNextStep(float stepSize, float bearing) {
        Log.d(TAG,"COMPUTE NEXT STEP");
        Location newLoc = mCurrentLocation;

        double oldX = mCurrentLocation.getXMeters();
        double oldY = mCurrentLocation.getYMeters();

        /*
        double oldLat = mCurrentLocation.getLatitude();

        double oldLng = mCurrentLocation.getLongitude();

        double newLat = Math.asin( Math.sin(Math.toRadians(oldLat))*Math.cos(angDistance) +
                Math.cos(Math.toRadians(oldLat))*Math.sin(angDistance)*Math.cos(bearing) );
        double newLon = Math.toRadians(oldLng) +
                Math.atan2(Math.sin(bearing)*Math.sin(angDistance)*Math.cos(Math.toRadians(oldLat)),
                        Math.cos(angDistance) - Math.sin(Math.toRadians(oldLat))*Math.sin(newLat));
         */

        //reconversion en degres

        Log.d(TAG,"STEP: " + stepSize);
        Log.d(TAG,"ANgle: " + bearing);
        Log.d(TAG,"COS ANgle: " + Math.cos(bearing));

        double newX = oldX + Math.cos(bearing) * stepSize;
        newLoc.setX(newX);
        double newY = oldY + Math.sin(bearing) * stepSize;
        newLoc.setY(newY);

        mCurrentLocation = newLoc;

        return newLoc;
    }

}