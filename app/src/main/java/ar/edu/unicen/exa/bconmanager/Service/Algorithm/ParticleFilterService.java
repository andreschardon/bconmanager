package ar.edu.unicen.exa.bconmanager.Service.Algorithm;

import android.content.Context;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.edu.unicen.exa.bconmanager.Adapters.ParticleFilterAdapter;
import ar.edu.unicen.exa.bconmanager.Model.AveragedTimestamp;
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap;
import ar.edu.unicen.exa.bconmanager.Model.CustomMap;
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataBeacon;
import ar.edu.unicen.exa.bconmanager.Model.Location;
import ar.edu.unicen.exa.bconmanager.Model.Particle;

public class ParticleFilterService extends Algorithm {

    //parameters
    private static final int NUM_PARTICLES = 200;
    private static final double STARTING_AREA_MTS = 0.5;
    private static final double RESAMPLING_MINIMUM = 0.90;

    private AtomicBoolean isActive = new AtomicBoolean(false);

    //particles
    public List<Particle> particles = null;

    //List of beacons obtained from Map
    List<BeaconOnMap> beaconsList;

    private double maxDist;

    //Fingerprinting position
    private double xPos;
    private double yPos;

    //moving delta
    private double movedX;
    private double movedY;

    //Estimated position
    private double estimateX;
    private double estimateY;

    //Weighted estimated position
    private double estimateWX;
    private double estimateWY;

    private ParticleFilterAdapter pfAdapter;
    private PDRService pdrService = PDRService.Companion.getInstance();
    private FingerprintingService referenceService = new FingerprintingService();

    public Location referenceLocation;
    public Location pfLocation;

    public boolean initialPosition = true;
    private boolean useFingerprinting = true;

    //private constructor
    public ParticleFilterService() {

    }

    @Override
    public void startUp(CustomMap customMap) {
        super.startUp(customMap);
        beaconsList = customMap.sortBeaconsByDistance(customMap.getSavedBeacons());

        referenceService.startUp(customMap);
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

        // set up particles
        particles = new ArrayList<>();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = new Particle();
            p.randomize(customMap.getWidth(), customMap.getHeight());
            particles.add(p);
        }
    }

    public void setStartingLocation(Location startingLocation) {
        // Overwrite starting point
        xPos = startingLocation.getXMeters();
        yPos = startingLocation.getYMeters();

        // A 4x4 square where we should create the particles
        double minWidth = xPos - STARTING_AREA_MTS;
        double minHeight = yPos - STARTING_AREA_MTS;
        double maxWidth = xPos + STARTING_AREA_MTS;
        double maxHeight = yPos + STARTING_AREA_MTS;

        // overwrite particles
        System.out.println("PFACTIVITY particle starting point "+startingLocation.toString());

        particles = new ArrayList<Particle>();
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = new Particle();
            p.randomize(minWidth, maxWidth, minHeight, maxHeight);
            particles.add(p);
        }
        this.printParticles();
        initialPosition = false;
    }

    public void setUseFingerprinting(boolean status) {
        useFingerprinting = status;
    }

    private void setUpParticleFilter(Context context, ParticleFilterAdapter pfAdapter) {
        this.pfAdapter = pfAdapter;

    }

    //singleton constructor
    private static ParticleFilterService instance = null;

    public static ParticleFilterService getInstance(Context context, CustomMap map, ParticleFilterAdapter pfAdapter) {
        if (instance == null) {
            instance = new ParticleFilterService();
            instance.startUp(map);
            instance.setUpParticleFilter(context, pfAdapter);

        }

        return instance;
    }

    @Override
    public Location getNextPosition(@NotNull AveragedTimestamp data) {

        if (initialPosition) {
            Location startingLocation = new Location(data.getPositionX(), data.getPositionY(), customMap);
            setStartingLocation(startingLocation);
        }

        Location pdrLocation = pdrService.getNextPosition(data);
        double movedX = pdrService.getMovedX();
        double movedY = pdrService.getMovedY();
        //System.out.println("PDRWTF Moved " + movedX + " and " + movedY);

        if (useFingerprinting) {
            Location referenceLocation = referenceService.getNextPosition(data);
            this.referenceLocation = referenceLocation;
            this.updatePosition(movedX, movedY, referenceLocation.getXMeters(), referenceLocation.getYMeters());
        } else {
            this.updatePosition(movedX, movedY, data.getBeacons());
        }

        Location result = new Location(estimateWX, estimateWY, customMap);
        this.pfLocation = result;
        return result;
    }

    /**
     * Updates user position with a new fingerprinting position and PDR
     */
    public void updatePosition(double movedXPDR, double movedYPDR, double xPosTrilat, double yPosTrilat) {

        this.movedX = movedXPDR;
        this.movedY = movedYPDR;
        this.xPos = xPosTrilat;
        this.yPos = yPosTrilat;
        this.applyFilter(null);
        this.sendBroadcastUserPosition();
    }

    /**
     * Updates user position with a PDR and new rssi values to approximate distances
     */
    public void updatePosition(double movedX, double movedY, List<JsonDataBeacon> beacons) {
        this.movedX = movedX;
        this.movedY = movedY;
        this.applyFilter(beacons);
        this.sendBroadcastUserPosition();
    }

    /**
     * return if the service is running
     *
     * @return
     */
    public boolean isRunning() {
        return isActive.get();
    }

    /**
     * stop thread update position
     */
    public void stop() {
        if (!isActive.get()) return;
        isActive.set(false);
    }

    public void applyFilter(List<JsonDataBeacon> beacons) {

        // 1. Move particles according to PDR (movedX, movedY)
        moveParticles();

        // 2. Weight particles according to reference point (fingerprint) or distances to beacons
        weightParticles(beacons);

        //printParticles();

        // 3. Resampling (delete old particles, generate new)
        resampleParticles();

        // 4. Weight particles again after resampling
        weightParticles(beacons);

        //printParticles();

        // 5. Calculate average point to return
        calculateAveragePoint();

        // 6. Clear any movedX, movedY values
        this.movedX = 0;
        this.movedY = 0;


    }

    private void printParticles() {
        for (int i = 0; i < particles.size(); i++) {
            Particle it = particles.get(i);
            System.out.println("PFACTIVITY particle[" + i + "]  loc (" + it.x + ", " + it.y + ") weight " + it.weight);
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

            //Also move reference point?
            this.xPos += this.movedX;
            this.yPos += this.movedY;
        }
    }

    private double weightParticles(List<JsonDataBeacon> beacons) {
        double maxWeight = 0;
        for (int i = 0; i < particles.size(); i++) {
            double weightSum = 0;
            //for every beacon?

            for (int j = 0; j < beaconsList.size(); j++) {

                // get distance to beacon of both the particle and the robot
                //Replace with trilateration calculated position
                Location beaconLocation = beaconsList.get(j).getPosition();


                //this.xPos and this.yPos are the position provided by fingerprint
                double userDistToBeacon = 0.0;
                if (beacons == null) {
                    // Fingerprinting way
                    userDistToBeacon = distance(this.xPos, this.yPos, beaconLocation.getXMeters(), beaconLocation.getYMeters());
                } else {
                    // Distance approximation way
                    String beaconAddres = beaconsList.get(j).getBeacon().getAddress();
                    JsonDataBeacon toFind = new JsonDataBeacon(beaconAddres, 0.0, 0);
                    JsonDataBeacon datasetBeacon = beacons.get(beacons.indexOf(toFind));

                    Integer txPower = null;
                    switch(beaconAddres)
                    {
                        case "0C:F3:EE:0D:84:50":
                            txPower = -60;
                            break;
                        case "DF:B5:15:8C:D8:35":
                            txPower = -60;
                            break;
                        case "D3:B5:67:2B:92:DA":
                            txPower = -60;
                            break;
                        case "C1:31:86:2A:30:62":
                            txPower = -60;
                            break;
                        default:
                            txPower = null;
                    }

                    userDistToBeacon = calculateDistance(datasetBeacon.getRssi(), txPower);
                }

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

    private double calculateDistance(double rssi, Integer txPower) {
        if (txPower == null) {
            txPower = -60;
        }
        // d = 10 ^ ((TxPower - RSSI) / 40)
        return ((Math.pow(10.0, ((txPower - rssi) / 40f))));
    }

    private void resampleParticles() {
        List<Particle> newParticles = new ArrayList<>();
        int numParticles = particles.size();
        Log.d("PFACTIVITY", "RESAMPLING -- NUM PARTICLES: " + numParticles);
        int previousValid = -1;
        int accumToAdd = 0;
        //RESAMPLING_MINIMUM *= RESAMPLING_MINIMUM;

        for (int i = 0; i < numParticles; i++) {
            if (particles.get(i).weight < RESAMPLING_MINIMUM) {
                newParticles.add(particles.get(i));
                previousValid = i;
            } else {
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