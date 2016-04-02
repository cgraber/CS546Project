package utils;

/**
 * Created by daeyun on 4/1/16.
 */
public class Metric {
    private Double accuracy;
    private Double precision;
    private Double recall;

    public Metric() {
        accuracy = precision = recall = null;
    }

    public double f1() {
        return 2 * precision * recall / (precision + recall);
    }

    public double accuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double precision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double recall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    @Override
    public String toString() {
        return String.format("Accuracy: %2.3f, Precision: %2.3f, Recall %2.3f, F1 = %2.3f",
                accuracy(), precision(), recall(), f1());
    }
}
