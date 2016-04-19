package experiments;

import data.*;
import learn.FeatureVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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


        //training  stage
        NaiveBayes nb_classifier = new NaiveBayes();
        List<FeatureVector> train_extract_data = REFeatures.generateFeatures(train_set, 0.97f);
        nb_classifier.train(train_extract_data);



        //iterate through all sentence instance
        for(GISentence g: test_sentences){

            g.assignPredictionWithCorefConstraint(nb_classifier);

            //mode = 1, automatically set coreference to NO_RELATION
            //g.assignPrediction(nb_classifier, 1);

        }


        //put real label by checking goldRelation
        for(GISentence g: test_sentences){
           GISentence.assignTrueLabel(g);
        }

        GISentence.printGiInformation (test_sentences);
        GISentence.ResultSummary(test_sentences);




    }


}
