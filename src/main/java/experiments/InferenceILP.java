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



        //Add more relation
        //List<GISentence> train_sentences = GISentence.BreakDocumentIntoSentence(train_set, 1);
        //GISentence.IncrementRelationFromCoref(train_sentences);

        List<GISentence> test_sentences = GISentence.BreakDocumentIntoSentence(test_set, 1);
        //GISentence.IncrementRelationFromCoref(test_sentences);





        //training  stage
        NaiveBayes nb_classifier = new NaiveBayes();
        List<FeatureVector> train_extract_data = ReFeatures.generateFeatures(train_set, "train");
        nb_classifier.train(train_extract_data);







        //iterate through all sentence instance
        for(GISentence g: test_sentences){

            //mode = 0 compare max, mode = 1 compare acc max
            //g.assignRelationWithCorefConstraint(nb_classifier, 1);

            //mode = 1, automatically set coreference to NO_RELATION
            g.assignRelation(nb_classifier, 0);

        }


        //put real label by checking goldRelation
        for(GISentence g: test_sentences){
           GISentence.assignTrueLabel(g);
        }

        GISentence.printGiInformation (test_sentences);







        //summary
        int hit = 0;
        int count = 0;


        int labels_count = Relation.labels_count;
        int [] c_pick = new int [labels_count];
        int [] c_hit = new int [labels_count];
        int [] c_count = new int [labels_count];

        for(GISentence g: test_sentences){
            for(Relation r: g.relations){

                if (r.pred_num == r.type_num) {
                    hit++;
                    c_hit[r.type_num]++;
                }
                count++;
                c_pick[r.pred_num]++;
                c_count[r.type_num]++;

            }
        }

        System.out.println("\nacc"+ (float)hit/count);
        for(int i=0;i<labels_count;i++){

            System.out.print("Class "+i+" ");
            System.out.print("Count "+c_count[i]+" ");
            System.out.print("Pick "+c_pick[i]+" ");
            System.out.print("Hit "+c_hit[i]+" ");
            System.out.println((float)c_hit[i]/c_count[i]);

        }






    }


}
