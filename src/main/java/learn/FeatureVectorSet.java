package learn;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sdq on 3/24/16.
 */
public class FeatureVectorSet implements Serializable {

    private static final long serialVersionUID = 6L;

    //basic information
    private int features_count=0;
    private int labels_count=0;
    private List<FeatureVector> set=new ArrayList<>();

    public FeatureVectorSet(List<FeatureVector> input_set, int f_count, int l_count){
        this.set=input_set;
        this.features_count=f_count;
        this.labels_count=l_count;

    }

    public List<FeatureVector> getList(){return set;}
    public int getFeaturesCount(){return features_count;}
    public int getLabelsCount(){return labels_count;}

    public void writeToFile() throws IOException {
        File fout = new File("features_data/data_set");
        fout.getParentFile().mkdirs();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fout));
        oos.writeObject(this);
        oos.close();
    }

    public static FeatureVectorSet readFromFile() throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("features_data/data_set")));
        FeatureVectorSet result = null;
        try {
            result = (FeatureVectorSet) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

}
