package ar.edu.unicen.exa.bconmanager.Service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap;
import ar.edu.unicen.exa.bconmanager.Model.CustomMap;
import ar.edu.unicen.exa.bconmanager.Model.Location;
import ar.edu.unicen.exa.bconmanager.Model.Particle;

public class ParticleFilterService {

    //paramenters
    private static final int FPS = 10;
    private static final int NUM_PARTICLES = 100;
    private static final int R_WALK_MAX = 50;
    private static final int R_WALK_FREQUENCY = 5;
    private static final double JUMP_DISTANCE = 40;

    private AtomicBoolean isActive = new AtomicBoolean(false);
    private Context context;

    //particles
    private List<Particle> particles = null;

    //List of beacons obtained from Map
    List<BeaconOnMap> beaconsList;

    private int curFrame;
    private double maxDist;

    //trilateration position
    private double xPos;
    private double yPos;

    //moving delta
    private double movedX;
    private double movedY;

    //estimate posizion
    private double estimateX;
    private double estimateY;

    //weight estimate position
    private double estimateWX;
    private double estimateWY;

    //private constructor
    private ParticleFilterService(Context context, CustomMap map) {
        this.context = context;
        Log.d("PARTICLEFILTERSERVICE","CONSTRUCTOR");
        double maxRangeHeight = map.getHeight();
        double maxRangeWidth = map.getWidth();

        /** Calculate the three closest circles **/
        Log.d("SAVED", "${map.savedBeacons}");
        beaconsList = map.sortBeaconsByDistance();

        maxDist = Math.floor(Math.sqrt(maxRangeWidth * maxRangeWidth + maxRangeHeight * maxRangeHeight));

        xPos = Math.floor(Math.random() * maxRangeWidth);
        yPos = Math.floor(Math.random() * maxRangeHeight);

        movedX = 0;
        movedY = 0;
        estimateX = 0;
        estimateY = 0;
        estimateWX = 0;
        estimateWY = 0;
        curFrame = -1;

        // set up particles
        particles = new ArrayList<Particle>();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = new Particle();
            p.randomize(maxRangeWidth, maxRangeHeight);
            particles.add(p);
        }
    }

    //singleton constructor
    private static ParticleFilterService instance = null;

    public static ParticleFilterService getInstance(Context context,CustomMap map) {
        if (instance == null) {
            instance = new ParticleFilterService(context,map);
        }

        return instance;
    }

    /**
     * update user position witha new trilateratio position
     *
     * @param xPos
     * @param yPos
     */
    public void updatePosition(double xPos, double yPos) {

        this.movedX = xPos - this.xPos;
        this.movedY = yPos - this.yPos;

        // if mouse moved too much, consider it a jump (a jump will not update the
        //	movement of the particles)
        if (Math.abs(this.movedX) > JUMP_DISTANCE || Math.abs(this.movedY) > JUMP_DISTANCE)
            this.movedX = this.movedY = 0;

        this.xPos = xPos;
        this.yPos = yPos;

        System.out.println("-> User position: " + this.xPos + " - " + this.yPos);
    }

    /**
     * return if the service is running
     *
     * @return
     */
    public boolean inRunning() {
        return isActive.get();
    }

    /**
     * start thread update position
     */
    public void start() {
        if (isActive.get()) return;
        isActive.set(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isActive.get()) {
                    //applica il filtro ed invia il broadcast
                    applyFilter();
                    sendBroadcastUserPosition();

                    //controllo di disattivazione servizio ogni 500 frames
                    if ((curFrame % 500) == 0) {
                        //CHECK!!
                            isActive.set(false);
                    }

                    try {
                        Thread.sleep(1000 / FPS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    /**
     * stop thread update position
     */
    public void stop() {
        if (!isActive.get()) return;
        isActive.set(false);
    }

    /**
     * Particle Filter: run the actual particle filter algorithm here
     */
    public void applyFilter() {

        //increment frame counter
        this.curFrame += 1;

        // 0. approximate robot position using current particles
        double totalX = 0;
        double totalY = 0;
        double totalWX = 0;
        double totalWY = 0;
        double totalW = 0;
        for (int i = 0; i < particles.size(); i++) {
            totalX += particles.get(i).x;
            totalY += particles.get(i).y;
            double weight = particles.get(i).getWeight();
            totalWX += (weight * particles.get(i).x);
            totalWY += (weight * particles.get(i).y);
            totalW += weight;
        }
        // direct average location of all particles
        this.estimateX = Math.floor(totalX / particles.size());
        this.estimateY = Math.floor(totalY / particles.size());
        // weighted average of all particles
        this.estimateWX = Math.floor(totalWX / totalW);
        this.estimateWY = Math.floor(totalWY / totalW);

        // 1. if mouse moved (i.e. the "agent" moved), update all particles
        //	by the same amount as the mouse movement
        if (this.movedX != 0 || this.movedY != 0) {
            for (int i = 0; i < particles.size(); i++) {

                //Replace movedX for distance un x, PDR next location¿?
                particles.get(i).x += this.movedX;
                particles.get(i).y += this.movedY;
            }
        }

        // 2. do a random walk if on random walk frame

        //UPDATES PARTICLE POSITION WITH RANDOM WALK ??

        if (R_WALK_FREQUENCY != 0 && (this.curFrame % R_WALK_FREQUENCY) == 0) {
            for (int i = 0; i < particles.size(); i++) {
                double dX = Math.floor(Math.random() * (R_WALK_MAX + 1)) - R_WALK_MAX / 2;
                double dY = Math.floor(Math.random() * (R_WALK_MAX + 1)) - R_WALK_MAX / 2;
                particles.get(i).x += dX;
                particles.get(i).y += dY;
            }
        }

        // 3. estimate weights of every particle
        double maxWeight = 0;
        for (int i = 0; i < particles.size(); i++) {
            double weightSum = 0;
            //for every beacon?

            for (int j = 0; j < beaconsList.size(); j++) {

                // get distance to beacon of both the particle and the robot
                //Replace with trilateration calculated position
                Location beaconLocation = beaconsList.get(j).getPosition();


                //this.xPos and this.yPos are the position provided by trilateration
                //apLocation is the position of the beacon

                double userDistToBeacon = distance(this.xPos, this.yPos, beaconLocation.getX(), beaconLocation.getY());
                double particleDistToBeacon = distance(particles.get(i).x, particles.get(i).y, beaconLocation.getX(), beaconLocation.getY());

                //particle distance to beacon is known, userDistToBeacon
                weightSum += getWeight(userDistToBeacon, particleDistToBeacon);
            }
            //beacons.size
            double weight = weightSum / beaconsList.size();
            particles.get(i).degrade(weight);
            if (weight > maxWeight)
                maxWeight = weight;
        }

        // 4. normalize weights
        double weightSum = 0;
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).normalize(maxWeight);
            weightSum += particles.get(i).getWeight();
        }

        // 5. resample: pick each particle based on probability
        List<Particle> newParticles = new ArrayList<>();
        int numParticles = particles.size();
        Log.d("NUM","NUM PARTICLES: "+numParticles);
        for (int i = 0; i < numParticles; i++) {
            double choice = Math.random() * weightSum;
            int index = 0;
            Log.d("NUM","INDEX: "+index);
            while (index < numParticles && choice > 0)  {
                Log.d("NUM","INDEX: "+index);
                choice -= particles.get(index).getWeight();
                index++;
            }
            newParticles.add(particles.get(index).clone());
        }
        particles = newParticles;

        // clear any movedX, movedY values
        this.movedX = 0;
        this.movedY = 0;
    }

    /**
     * Distance Formula: returns the distance between two given points
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private double distance(double x1, double y1, double x2, double y2) {
        double distX = x1 - x2;
        double distY = y1 - y2;
        return Math.floor(Math.sqrt(distX * distX + distY * distY));
    }

    /**
     * computes distance weight between a
     *
     * @param robotDistToBeacon
     * @param particleDistToBeacon
     * @return
     */
    private double getWeight(double robotDistToBeacon, double particleDistToBeacon) {
        double diff = Math.abs(robotDistToBeacon - particleDistToBeacon);
        return (maxDist - diff) / maxDist;
    }

    /**
     * send a broadcast with a new user position
     */
    public void sendBroadcastUserPosition() {
        Intent broadcastReceiverIntent = new Intent();
        broadcastReceiverIntent.putExtra("x", (float) estimateWX);
        broadcastReceiverIntent.putExtra("y", (float) estimateWY);
        broadcastReceiverIntent.setAction("android.intent.action.UPDATE_USER_POSITION");
        context.sendBroadcast(broadcastReceiverIntent);

        System.out.println(String.format("posizione: %s - %s", estimateWX, estimateWY));
    }
}