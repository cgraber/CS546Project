package experiments;

import data.ACEAnnotation;
import data.DataUtils;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;

import java.io.IOException;
import learn.FeatureVector;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin Graber on 3/10/16.
 */
public class Baseline {

    public static void main(String [] argv) throws IOException{

        //List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits("./ACE05_English");
        //ACEAnnotation.writeAlltoFile(splits);

        List<FeatureVector> data=new ArrayList<>();
        for(int i=0;i<5;i++){

            FeatureVector vec=new FeatureVector();
            vec.addBinaryFeature("555"+i);

            vec.addlabelCount("label"+i);

            data.add(vec);
        }

        for(int i=0;i<5;i++){

            System.out.println(data.get(i).getFeatures());
            System.out.println(data.get(i).getLabel());

        }


    }
}
