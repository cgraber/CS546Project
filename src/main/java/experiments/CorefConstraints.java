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
public class CorefConstraints {

    public static void main(String [] argv) throws IOException {


        //load data
        List<ACEAnnotation> all_documents = DataStorage.LoadDocuments();
        Collections.shuffle(all_documents);

        //adding gold relation for corefgroup in each sentence
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



        //build GIsentence for coreference
        List<GISentence> test_sentences = GISentence.BreakDocumentIntoSentence(test_set, 1);


        //A2 force mention in same coreference group have the same relation with other coreference group
        //A1 force mention within same group have no_relaiton
        //A2 include A1
        boolean coref_A1=false;
        boolean coref_A2=false;


        //Add more relation from coreference group
        if(coref_A2){
            List<GISentence> train_sentences = GISentence.BreakDocumentIntoSentence(train_set, 1);
            GISentence.IncrementRelationFromCoref(train_sentences);
            GISentence.IncrementRelationFromCoref(test_sentences);
        }


        //training  stage
        NaiveBayes nb_classifier = new NaiveBayes();
        List<FeatureVector> train_extract_data = ReFeatures.generateFeatures(train_set, 0.97f);
        nb_classifier.train(train_extract_data);


        //iterate through all sentence instance
        for(GISentence g: test_sentences){

            if(coref_A2)
                g.assignPredictionWithCorefConstraint(nb_classifier);
            else if(coref_A1)
                g.assignPrediction(nb_classifier, 1);
            else
                g.assignPrediction(nb_classifier,0);

        }


        //put real label by checking goldRelation
        for(GISentence g: test_sentences){
           GISentence.assignTrueLabel(g);
        }

        GISentence.printGiInformation (test_sentences);
        GISentence.ResultSummary(test_sentences);




    }


}
