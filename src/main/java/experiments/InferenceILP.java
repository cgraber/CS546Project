package experiments;

import data.*;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import learn.FeatureVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Created by sdq on 4/11/16.
 */
public class InferenceILP {

    public static void main(String [] argv) throws IOException {

        List<ACEAnnotation> all_documents = ACEAnnotation.readAllFromFileFlat();
        Collections.shuffle(all_documents);

        List<ACEAnnotation> train_set = new ArrayList<>();
        List<ACEAnnotation> test_set = new ArrayList<>();

        //split data
        int train_size = (all_documents.size()*4)/5;
        for(int i=0;i<all_documents.size();i++){
            if(i<train_size)
                train_set.add(all_documents.get(i));
            else
                test_set.add(all_documents.get(i));
        }

        NaiveBayes nb_classifier = new NaiveBayes();

        List<FeatureVector> train_extract_data = ReFeatures.generateFeatures(train_set, "train");
        nb_classifier.train(train_extract_data);

        List<GISentence> gi_sentences = GISentence.BreakDocumentIntoSentence(test_set);



        //iterate through all sentence instance
        for(GISentence g: gi_sentences){
            g.assignRelationWithCorefConstraint(nb_classifier);
        }

        GISentence.printGiInformation (gi_sentences);



    }


}
