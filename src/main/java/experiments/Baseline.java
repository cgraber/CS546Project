package experiments;

import data.ACEAnnotation;
import data.DataUtils;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin Graber on 3/10/16.
 */
public class Baseline {

    public static void main(String [] argv) {

        List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits("./ACE05_English");
        NERBaseline ner = new NERBaseline();
        List<ACEAnnotation> train = new ArrayList<ACEAnnotation>();
        for (int i = 0; i < splits.size() - 1; i++) {
            train.addAll(splits.get(i));
        }
        List<ACEAnnotation> test = splits.get(splits.size()-1);
        ner.trainModel(train);
        ner.test(test);
        Pair<Double,Double> results = ner.evaluate(test);
        System.out.println("PRECISION: "+results.getFirst());
        System.out.println("RECALL: "+results.getSecond());
    }
}
