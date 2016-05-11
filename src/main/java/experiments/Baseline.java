package experiments;

import data.ACEAnnotation;
import data.DataUtils;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin Graber on 3/10/16.
 */
public class Baseline {

    public static void main(String [] argv) {
        if (argv.length != 1) {
            System.err.println("Missing arg: directory containing ACE2005 data");
            System.exit(1);
        }
        
	/*
        List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits(argv[0]);

        try {
            ACEAnnotation.writeAlltoFile(splits);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
*/
        List<List<ACEAnnotation>> splits = null;
        try {
            splits = ACEAnnotation.readAllFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }



        NERBaseline ner = new NERBaseline(false);
        List<ACEAnnotation> train = new ArrayList<ACEAnnotation>();
        for (int i = 0; i < splits.size() - 1; i++) {
            train.addAll(splits.get(i));
        }
        List<ACEAnnotation> test = splits.get(splits.size() - 1);
        ner.trainModel(train);
        ner.test(test);
        Pair<Double,Double> results = ner.evaluateHead(test);
        System.out.println("HEAD PRECISION: "+results.getFirst());
        System.out.println("HEAD RECALL: " + results.getSecond());
        double headF1 = 2*results.getSecond()*results.getFirst()/(results.getFirst()+results.getSecond());
        System.out.println("HEAD F1: "+headF1);
        System.out.println("");

        results = ner.evaluateExtent(test);
        System.out.println("EXTENT PRECISION: "+results.getFirst());
        System.out.println("EXTENT RECALL: "+results.getSecond());
        double extentF1 = 2*results.getSecond()*results.getFirst()/(results.getFirst()+results.getSecond());
        System.out.println("EXTENT F1: "+extentF1);
        System.out.println("");
    }
}
