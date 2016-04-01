package learn;

import data.EntityMention;

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
    private static Map<String, Integer> labelMap;
    private static List<String> stringList;

    static {
        featureMap = new HashMap<>();
        labelMap = new HashMap<>();
        stringList=new ArrayList<>();
    }

    //All of the features default to zero
    private List<Integer> features = new ArrayList<>(Collections.nCopies(getFeatureCount(), 0));

    public EntityMention left;
    public EntityMention right;
    public List<String> sentence;

    //-1 stand for no label for this instance
    private int label = -1;
    private String labelString;

    public void addRelationMetadata(EntityMention left, EntityMention right, List<String> sentence) {
        this.left=left;
        this.right=right;
        this.sentence=sentence;
    }

    public void addBinaryFeature(String featureName) {
        if (!featureMap.containsKey(featureName)) {
            featureMap.put(featureName, featureMap.size());
            updateFeatureVectorSize();
        }
        features.set(featureMap.get(featureName), 1);
    }

    public void addLabel(String labelName){
        labelString =labelName;
        if(!labelMap.containsKey(labelName)){
            label = labelMap.size();
            labelMap.put(labelName, label);
            stringList.add(labelString);
        }
        label = labelMap.get(labelName);
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
    public int getLabelCount(){return labelMap.size();}
    public String getLabelString(){return labelString;}
    public List<String> getStringList(){return stringList;}
    public Map<String, Integer> getLabelMap(){return labelMap;}
    public Map<String, Integer> getFeatureMap(){return featureMap;}
    public List<String> getSentence(){return sentence;}
}
