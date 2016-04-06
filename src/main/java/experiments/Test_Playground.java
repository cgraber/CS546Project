package experiments;

import data.ACEAnnotation;
import data.EntityMention;
import data.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.lbjava.nlp.StringArraysToWords;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static java.lang.Math.log;

/**
 * Created by sdq on 3/23/16.
 */
public class Test_Playground {

    public static void main(String [] argv) throws IOException {

        ACEAnnotation test = ACEAnnotation.readFileByID(5);
        List<List<String>> postag_s = test.getPOSTagsBySentence();
        List<String> postag = postag_s.get(0);
        List<String> sentence = test.getSentence(0);

        for(String x:sentence){

            System.out.print(x+" ");

        }

        System.out.println();

        for(String x:postag){

            System.out.print(x+" ");

        }

        System.out.println(ACEAnnotation.getEntityTypes());





    }



}
