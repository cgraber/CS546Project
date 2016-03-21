package learn;

import java.util.*;

/**
 * Created by Colin Graber on 3/21/16.
 */
public class FeatureVector {
    //This contains the mapping between the feature name and its index within the feature vector
    //It is global so that feature indices are consistent between indices
    private static Map<String, Integer> featureMap;
    private static int featureCount;
    static {
        featureMap = new HashMap<>();
        featureCount = 0;
    }

    //All of the features default to zero
    private List<Integer> features = new ArrayList<>(Collections.nCopies(featureCount, 0));

    public void addBinaryFeature(String featureName) {
        if (!featureMap.containsKey(featureName)) {
            featureMap.put(featureName, featureCount++);
            features.add(0);
        }
        features.set(featureMap.get(featureName), 1);
    }

    public List<Integer> getFeatures() {
        return features;
    }
}
