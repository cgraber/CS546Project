package learn;

import java.util.*;

/**
 * Created by Colin Graber on 3/21/16.
 */
public class FeatureVector {
    //This contains the mapping between the feature name and its index within the feature vector
    //It is global so that feature indices are consistent between indices
    private static Map<String, Integer> featureMap;
    private static Map<String, Integer> labelMap;
    private static int featureCount;
    private static int labelCount;

    static {
        featureMap = new HashMap<>();
        labelMap = new HashMap<>();
        featureCount = 0;
        labelCount = 0;
    }

    //All of the features default to zero
    private List<Integer> features = new ArrayList<>(Collections.nCopies(featureCount, 0));

    //-1 stand for no label for this instance
    private int label = -1;

    public void addBinaryFeature(String featureName) {
        if (!featureMap.containsKey(featureName)) {
            featureMap.put(featureName, featureCount++);
            features.add(0);
        }
        features.set(featureMap.get(featureName), 1);
    }

    public void addlabelCount(String labelName){
        if(!labelMap.containsKey(labelName)){
            labelMap.put(labelName, labelCount++);
            label=labelCount;
        }
        label=labelMap.get(labelName);
    }

    public List<Integer> getFeatures() {
        return features;
    }
    public int getLabel(){ return label; }

    public int getFeatureCount(){return featureCount;}
    public int getLabelCount(){return labelCount;}

}
