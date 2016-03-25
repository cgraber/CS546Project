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
    private static int featureCount;
    private static int labelCount;

    static {
        featureMap = new HashMap<>();
        labelMap = new HashMap<>();
        stringList=new ArrayList<>();
        featureCount = 0;
        labelCount = 0;
    }

    //All of the features default to zero
    private List<Integer> features = new ArrayList<>(Collections.nCopies(featureCount, 0));

    public EntityMention left;
    public EntityMention right;
    public List<String> sentence;

    //-1 stand for no label for this instance
    private int label = -1;
    private String label_string;

    public void addInformation(EntityMention left, EntityMention right, List<String> sentence){

        this.left=left;
        this.right=right;
        this.sentence=sentence;

    }

    public void addBinaryFeature(String featureName) {
        if (!featureMap.containsKey(featureName)) {
            featureMap.put(featureName, featureCount++);
            features.add(0);
        }
        features.set(featureMap.get(featureName), 1);
    }

    public void addLabel(String labelName){
        label_string=labelName;
        if(!labelMap.containsKey(labelName)){

            label=labelCount;
            labelMap.put(labelName, labelCount);
            labelCount++;
            stringList.add(label_string);

        }
        label=labelMap.get(labelName);
    }

    public List<Integer> getFeatures() {
        return features;
    }
    public int getLabel(){ return label; }

    public int getFeatureCount(){return featureCount;}
    public int getLabelCount(){return labelCount;}
    public String getLabelString(){return label_string;}
    public List<String> getStringList(){return stringList;}
    public Map<String, Integer> getLabelMap(){return labelMap;}
    public List<String> getSentence(){return sentence;}


}
