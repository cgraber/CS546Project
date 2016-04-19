package learn;

import data.EntityMention;
import data.Relation;

import java.io.*;
import java.util.*;

/**
 * Created by Colin Graber on 3/21/16.
 */
public class FeatureVector implements Serializable {
    //This contains the mapping between the feature name and its index within the feature vector
    //It is global so that feature indices are consistent between indices

    private static final long serialVersionUID = 5L;

    private static Map<String, Integer> featureMap;

    static {
        featureMap = new HashMap<>();
    }

    //All of the features default to zero
    private List<Integer> features = new ArrayList<>(Collections.nCopies(getFeatureCount(), 0));



    //-1 stand for no label for this instance
    private int label = -1;
    private String labelString;



    public void addBinaryFeature(String featureName) {
        if (!featureMap.containsKey(featureName)) {
            featureMap.put(featureName, featureMap.size());
            updateFeatureVectorSize();
        }
        features.set(featureMap.get(featureName), 1);
    }

    public void addLabel(String labelName){
        labelString =labelName;
        label = Relation.labelMap.get(labelName);
    }

    public List<Integer> getFeatures() {
        updateFeatureVectorSize();
        return features;
    }

    private void updateFeatureVectorSize() {
        if (getFeatureCount() != features.size()) {
            features.addAll(new ArrayList<>(Collections.nCopies(getFeatureCount() - features.size(), 0)));
        }
    }

    public int getLabel(){ return label; }

    public List<Integer> getLabelVector() {
        List<Integer> labelVector = new ArrayList<>(Collections.nCopies(getLabelCount(), 0));

        // Returns a one-hot. This should change if we support multiple labels.
        labelVector.set(label, 1);
        return labelVector;
    }

    public int getFeatureCount(){return featureMap.size();}
    public int getLabelCount(){return Relation.labelMap.size();}
    public List<String> getStringList(){return Relation.stringList;}
    public Map<String, Integer> getLabelMap(){return Relation.labelMap;}

}
