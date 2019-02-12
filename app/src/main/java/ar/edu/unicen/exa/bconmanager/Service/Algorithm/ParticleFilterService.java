package ar.edu.unicen.exa.bconmanager.Service.Algorithm;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.edu.unicen.exa.bconmanager.Adapters.ParticleFilterAdapter;
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap;
import ar.edu.unicen.exa.bconmanager.Model.CustomMap;
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData;
import ar.edu.unicen.exa.bconmanager.Model.Location;
import ar.edu.unicen.exa.bconmanager.Model.Particle;

public class ParticleFilterService extends Algorithm {

    //paramenters
    private static final int FPS = 10;
    private static final int NUM_PARTICLES = 100;
    private static final int R_WALK_MAX = 50;
    private static final int R_WALK_FREQUENCY = 5;
    private static final double JUMP_DISTANCE = 40;
    private static final double STARTING_AREA_MTS = 2.0;
    private double RESAMPLING_MINIMUM = 0.9;

    private AtomicBoolean isActive = new AtomicBoolean(false);
    private Context context;

    //particles
    public List<Particle> particles = null;

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

    private ParticleFilterAdapter pfAdapter;
    //private TrilaterationService trilaterationCalculator = TrilaterationService.Companion.getInstance();
    private PDRService pdrService = PDRService.Companion.getInstance();
    private FPTrilat referenceService = FPTrilat.Companion.getInstance();

    //public Location trilaterationLocation;
    public Location referenceLocation;
    public Location pfLocation;

    public boolean initialPosition = true;

    //private constructor
    public ParticleFilterService() {

    }

    @Override
    public void startUp(CustomMap customMap) {
        super.startUp(customMap);
        beaconsList = customMap.sortBeaconsByDistance(customMap.getSavedBeacons());



        referenceService.startUp(customMap);
        //trilaterationCalculator.startUp(customMap);



        pdrService.startUp(customMap);

        Log.d("SAVED", "${map.savedBeacons}");
        maxDist = Math.floor(Math.sqrt(customMap.getWidth() * customMap.getWidth() + customMap.getHeight() * customMap.getHeight()));

        xPos = Math.floor(Math.random() * customMap.getWidth());
        yPos = Math.floor(Math.random() * customMap.getHeight());

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
            p.randomize(customMap.getWidth(), customMap.getHeight());
            particles.add(p);
        }
    }

    private void setUpParticleFilter(Context context, ParticleFilterAdapter pfAdapter) {
        this.context = context;
        this.pfAdapter = pfAdapter;

    }

    //singleton constructor
    private static ParticleFilterService instance = null;

    public static ParticleFilterService getInstance(Context context,CustomMap map, ParticleFilterAdapter pfAdapter) {
        if (instance == null) {
            instance = new ParticleFilterService();
            instance.startUp(map);
            instance.setUpParticleFilter(context, pfAdapter);

        }

        return instance;
    }

    @Override
    public Location getNextPosition(@NotNull JsonData data, @NotNull Number nextTimestamp) {

        /*Location trilatLocation = trilaterationCalculator.getNextPosition(data, nextTimestamp);
        this.trilaterationLocation = trilatLocation;*/
        if (initialPosition) {
            Location startingLocation = new Location(data.getPositionX(),data.getPositionY(),customMap);
            // Overwrite starting point
            xPos = startingLocation.getXMeters();
            yPos = startingLocation.getYMeters();

            // A 4x4 square where we should create the particles
            double minWidth = xPos - STARTING_AREA_MTS;
            double minHeight = yPos - STARTING_AREA_MTS;
            double maxWidth = xPos + STARTING_AREA_MTS;
            double maxHeight = yPos + STARTING_AREA_MTS;

            // overwrite particles
            particles = new ArrayList<Particle>();
            for (int i = 0; i < NUM_PARTICLES; i++) {
                Particle p = new Particle();
                p.randomize(minWidth, maxWidth, minHeight, maxHeight);
                particles.add(p);
            }
            initialPosition = false;
        }


        Location referenceLocation = referenceService.getNextPosition(data, nextTimestamp);
        this.referenceLocation = referenceLocation;

        Location pdrLocation = pdrService.getNextPosition(data, nextTimestamp);
        double movedX = pdrService.getMovedX();
        double movedY = pdrService.getMovedY();
        System.out.println("PDRWTF Moved "+movedX+" and "+movedY);

        this.updatePosition(movedX, movedY, referenceLocation.getXMeters(), referenceLocation.getYMeters());
        Location result = new Location(estimateWX, estimateWY, customMap);
        this.pfLocation = result;
        return result;
    }

    /**
     * update user position witha new trilateratio position and PDR
     *
     */
    public void updatePosition(double movedXPDR, double movedYPDR, double xPosTrilat, double yPosTrilat) {

        this.movedX = movedXPDR;
        this.movedY = movedYPDR;

        // if mouse moved too much, consider it a jump (a jump will not update the
        //	movement of the particles)
        /*
        if (Math.abs(this.movedX) > JUMP_DISTANCE || Math.abs(this.movedY) > JUMP_DISTANCE)
            this.movedX = this.movedY = 0;
            */

        this.xPos = xPosTrilat;
        this.yPos = yPosTrilat;
        this.start();
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
        applyFilter();
        sendBroadcastUserPosition();
        /*
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
    */
    }

    /**
     * stop thread update position
     */
    public void stop() {
        if (!isActive.get()) return;
        isActive.set(false);
    }

    public void applyFilter() {

        // 1. Desplazar las partículas de acuerdo al PDR (movedX, movedY)
        moveParticles();

        // 2. Pesar las partículas respecto al punto de referencia (trilat)
        double weightSum = weightParticles();

        printParticles();

        // 3. Resampling (eliminar particulas lejanas, generar nuevas)
        resampleParticles();

        // 4. Pesar nuevamente las partículas
        weightSum = weightParticles();

        printParticles();

        // 5. Clear any movedX, movedY values
        this.movedX = 0;
        this.movedY = 0;

        // 6. Calculate average point to return
        calculateAveragePoint();


    }

    private void printParticles() {
        for (int i = 0; i < particles.size(); i++) {
            Particle it = particles.get(i);
            System.out.println("PFACTIVITY particle[" + i + "]  loc (" + it.x + ", " +  it.y + ") weight " + it.weight);
        }
    }

    private void moveParticles() {
        if (this.movedX != 0 || this.movedY != 0) {
            for (int i = 0; i < particles.size(); i++) {

                //Replace movedX for distance un x, PDR next location¿?
                particles.get(i).x += this.movedX;
                particles.get(i).y += this.movedY;
                particles.get(i).restrictToMap(customMap.getWidth(), customMap.getHeight());
                particles.get(i).restrictDecimals();
            }
        }
    }

    private double weightParticles() {
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

                double userDistToBeacon = distance(this.xPos, this.yPos, beaconLocation.getXMeters(), beaconLocation.getYMeters());
                double particleDistToBeacon = distance(particles.get(i).x, particles.get(i).y, beaconLocation.getXMeters(), beaconLocation.getYMeters());

                //particle distance to beacon is known, userDistToBeacon
                weightSum += getWeight(userDistToBeacon, particleDistToBeacon);
            }
            //beacons.size
            double weight = weightSum / beaconsList.size();
            // Degrade weight
            //particles.get(i).degrade(weight);
            particles.get(i).weight = weight;
            if (weight > maxWeight)
                maxWeight = weight;
        }

        // 4. normalize weights
        double weightSum = 0;
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).normalize(maxWeight);
            weightSum += particles.get(i).getWeight();
        }
        return weightSum;
    }

    private void resampleParticles() {
        List<Particle> newParticles = new ArrayList<>();
        int numParticles = particles.size();
        Log.d("PFACTIVITY","RESAMPLING -- NUM PARTICLES: " + numParticles );
        int previousValid = -1;
        int accumToAdd = 0;
        //RESAMPLING_MINIMUM *= RESAMPLING_MINIMUM;

        for (int i = 0; i < numParticles; i++) {
            if (particles.get(i).weight < RESAMPLING_MINIMUM) {
                newParticles.add(particles.get(i));
                previousValid = i;
            }   else {
                accumToAdd++;
                if (previousValid != -1) {
                    while (accumToAdd != 0) {
                        newParticles.add(cloneDistorted(particles.get(previousValid)));
                        accumToAdd--;
                    }
                }
            }
        }
        if (newParticles.size() != 0)
            particles = newParticles;
        else
            Log.e("PFACTIVITY", "NO MORE PARTICLES ------------------------------");
    }
















    /**
     * Particle Filter: run the actual particle filter algorithm here
     */
    public void applyFilter2() {


        //increment frame counter
        this.curFrame += 1;

        /*
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
        System.out.println("Estamos en apply filter " + this.estimateWX + " " + this.estimateWY);
        */


        // 1. if mouse moved (i.e. the "agent" moved), update all particles
        //	by the same amount as the mouse movement
        moveParticles();

        // 2. do a random walk if on random walk frame

        //UPDATES PARTICLE POSITION WITH RANDOM WALK ??
        /*
        if (R_WALK_FREQUENCY != 0 && (this.curFrame % R_WALK_FREQUENCY) == 0) {
            for (int i = 0; i < particles.size(); i++) {
                double dX = Math.floor(Math.random() * (R_WALK_MAX + 1)) - R_WALK_MAX / 2;
                double dY = Math.floor(Math.random() * (R_WALK_MAX + 1)) - R_WALK_MAX / 2;
                particles.get(i).x += dX;
                particles.get(i).y += dY;
            }
        }*/

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

                double userDistToBeacon = distance(this.xPos, this.yPos, beaconLocation.getXMeters(), beaconLocation.getYMeters());
                double particleDistToBeacon = distance(particles.get(i).x, particles.get(i).y, beaconLocation.getXMeters(), beaconLocation.getYMeters());

                //particle distance to beacon is known, userDistToBeacon
                weightSum += getWeight(userDistToBeacon, particleDistToBeacon);
            }
            //beacons.size
            double weight = weightSum / beaconsList.size();
            //particles.get(i).degrade(weight);
            particles.get(i).weight = weight;
            if (weight > maxWeight)
                maxWeight = weight;
        }

        // 4. normalize weights
        double weightSum = 0;
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).normalize(maxWeight);
            weightSum += particles.get(i).getWeight();
        }


        double lowestWeight = 9999.0;
        double lowestX = 0.0;
        double lowestY = 0.0;

        for (int i = 0; i < particles.size(); i++) {
            Particle it = particles.get(i);
            System.out.println("PFACTIVITY particle[" + i + "]  loc (" + it.x + ", " +  it.y + ") weight " + it.weight);
            if (it.weight < lowestWeight) {
                lowestWeight = it.weight;
                lowestX = it.x;
                lowestY = it.y;
            }
        }

        System.out.println("PFACTIVITY lowest point is (" + lowestX + ", " + lowestY + ") weight: " + lowestWeight);
        //calculateAveragePoint();

        // 5. resample: pick each particle based on probability
        // Not what we expected

        List<Particle> newParticles = new ArrayList<>();
        int numParticles = particles.size();
        Log.d("PFACTIVITY","NUM PARTICLES: " + numParticles + " weight sum: " + weightSum);
        /*for (int i = 0; i < numParticles; i++) {
            double choice = Math.random() * weightSum;
            int index = 0;
            Log.d("NUM","INDEX: "+index);
            while (index < numParticles && choice > 0)  {
                Log.d("NUM","INDEX: "+index);
                choice -= particles.get(index).getWeight();
                index++;
            }
            if (index < numParticles) {
                newParticles.add(particles.get(index).clone());
            }

        }
        Log.d("PFACTIVITY","NUM PARTICLES: " + newParticles.size());
        particles = newParticles;*/

        int previousValid = -1;
        int accumToAdd = 0;

        for (int i = 0; i < numParticles; i++) {
            if (particles.get(i).weight < RESAMPLING_MINIMUM) {
                newParticles.add(particles.get(i));
                previousValid = i;
            }   else {
                accumToAdd++;
                if (previousValid != -1) {
                    while (accumToAdd != 0) {
                        newParticles.add(cloneDistorted(particles.get(previousValid)));
                        accumToAdd--;
                    }
                }
            }

        }
        particles = newParticles;

        // 3. estimate weights of every particle
        maxWeight = 0;
        for (int i = 0; i < particles.size(); i++) {
            weightSum = 0;
            //for every beacon?

            for (int j = 0; j < beaconsList.size(); j++) {

                // get distance to beacon of both the particle and the robot
                //Replace with trilateration calculated position
                Location beaconLocation = beaconsList.get(j).getPosition();


                //this.xPos and this.yPos are the position provided by trilateration
                //apLocation is the position of the beacon

                double userDistToBeacon = distance(this.xPos, this.yPos, beaconLocation.getXMeters(), beaconLocation.getYMeters());
                double particleDistToBeacon = distance(particles.get(i).x, particles.get(i).y, beaconLocation.getXMeters(), beaconLocation.getYMeters());

                //particle distance to beacon is known, userDistToBeacon
                weightSum += getWeight(userDistToBeacon, particleDistToBeacon);
            }
            //beacons.size
            double weight = weightSum / beaconsList.size();
            //particles.get(i).degrade(weight);
            particles.get(i).weight = weight;
            if (weight > maxWeight)
                maxWeight = weight;
        }

        // 4. normalize weights
        weightSum = 0;
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).normalize(maxWeight);
            weightSum += particles.get(i).getWeight();
        }


        // clear any movedX, movedY values
        this.movedX = 0;
        this.movedY = 0;

        calculateAveragePoint();



    }

    private Particle cloneDistorted(Particle toClone) {
        Particle cloned = toClone.clone();
        // To review values 0.2
        double distortX = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        double distortY = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        System.out.println("Distortion is " + distortX + " and " + distortY);
        cloned.x += distortX;
        cloned.y += distortY;
        cloned.restrictToMap(customMap.getWidth(), customMap.getHeight());
        cloned.restrictDecimals();
        return cloned;

    }

    private void calculateAveragePoint() {
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
        this.estimateX = totalX / particles.size();
        this.estimateY = totalY / particles.size();
        // weighted average of all particles
        this.estimateWX = totalWX / totalW;
        this.estimateWY = totalWY / totalW;
        System.out.println(String.format("PFACTIVITY Estimate X Y   " + estimateX + ", " + estimateY));
        System.out.println(String.format("PFACTIVITY Estimate Wx Wy " + estimateWX + ", " + estimateWY));
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
        return (Math.sqrt(distX * distX + distY * distY));
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
        return diff; //(maxDist - diff) / maxDist;
    }

    /**
     * send a broadcast with a new user position
     */
    public void sendBroadcastUserPosition() {
        if (pfAdapter != null) {
            pfAdapter.setViewX(estimateWX);
            pfAdapter.setViewY(estimateWY);
            pfAdapter.notifyStepDetected();
        }
        System.out.println(String.format("PFACTIVITY PARTICLE FILTER POSITION IS " + estimateX + ", " + estimateY));
        System.out.println(String.format("PFACTIVITY PARTICLE FILTER POSITION IS " + estimateWX + ", " + estimateWY));
    }


}