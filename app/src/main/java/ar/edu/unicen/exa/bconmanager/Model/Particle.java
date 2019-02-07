package ar.edu.unicen.exa.bconmanager.Model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Particle {

    public double x = 0;
    public double y = 0;
    public double weight = 1.0;

    public Particle() {
    }

    // randomize particle position
    public void randomize(double maxWidth, double maxHeight) {
        this.x = Math.random() * maxWidth;
        this.y = Math.random() * maxHeight;
    }

    // randomize min max position
    public void randomize(double minWidth, double maxWidth, double minHeight, double maxHeight) {
        double width = maxWidth - minWidth;
        double height = maxHeight - minHeight;

        this.x = (Math.random() * width) + minWidth;
        this.y = (Math.random() * height) + minHeight;
    }

    // degrade weight: multiply current weight by the given amount
    //	(decimal between 0 and 1)
    public void degrade(double weight) {
        this.weight *= weight;
    }

    // normalizes weight around the maximum such that the particle with
    //	the most weight will now have a weight of 1.0, and all other
    //	particles' weights will scale accordingly
    public boolean normalize(double maxWeight) {
        if (maxWeight == 0) {
            System.out.println("error - maxWeight 0 (check weight function)");
            return false;
        }
        this.weight /= maxWeight;
        return true;
    }

    public void restrictToMap(double maxWidth, double maxHeight) {
        if (x > maxWidth) {
            x = maxWidth;
        } else if (x < 0) {
            x = 0;
        }

        if (y > maxHeight) {
            y = maxHeight;
        } else if (y < 0) {
            y = 0;
        }
    }

    public void restrictDecimals() {
        x = round(x, 2);
        y = round(y, 2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    // returns this particle's weight
    public double getWeight() {
        return this.weight;
    }

    // returns a NEW particle object with position and weight identical
    //	to this one
    public Particle clone() {
        Particle copy = new Particle();
        copy.x = this.x;
        copy.y = this.y;
        copy.weight = this.weight;
        return copy;
    }
}