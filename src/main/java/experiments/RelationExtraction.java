package experiments;

import data.ACEAnnotation;
import data.EntityMention;
import data.Relation;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import learn.FeatureVector;
import org.apache.commons.lang.math.NumberUtils;

import learn.FeatureVectorSet;

import weka.core.Instances;
import weka.classifiers.functions.Logistic;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.lang.*;

/**
 * Created by sdq on 3/22/16.
 */

public class RelationExtraction {

    public static void main(String [] argv) throws IOException {


        List<ACEAnnotation> collection = ACEAnnotation.readAllFromFileFlat();

        //features and labels for all data
        List<FeatureVector> extracted_data=new ArrayList<>();


        //process one document at a time
        for(ACEAnnotation document: collection) {

            List<EntityMention> gold_m = document.getGoldEntityMentions();

            //get all possible relation from this document (including sorting)
            List<List<EntityMention>> gold_m_sentence = document.splitMentionBySentence(gold_m);
            List<Pair<EntityMention, EntityMention>> possible_pair = ACEAnnotation.getPossibleMentionPair(gold_m_sentence);

            //get Lemmas and POSTags for feature extraction
            List<String> lemmas = document.getLemmas();
            List<String> pos_tags = document.getPOSTags();

            //get gold relations
            Map<Pair<EntityMention, EntityMention>, Relation> gold_relation = document.getGoldRelationsByArgs();

            //setup vector for storing features
            FeatureVector fea_vec = new FeatureVector();



            //features builder on all possible relation
            for (Pair<EntityMention, EntityMention> p : possible_pair) {

                //Entity based features
                EntityMention left = p.getFirst();
                EntityMention right = p.getSecond();

                //System.out.println(left.getExtent());
                //System.out.println(right.getExtent());

                fea_vec.addBinaryFeature("E1_type:" + left.getEntityType());
                fea_vec.addBinaryFeature("E2_type:" + right.getEntityType());
                fea_vec.addBinaryFeature("E1_head:" + lemmas.get(left.getEndOffset() - 1));
                fea_vec.addBinaryFeature("E2_head:" + lemmas.get(right.getEndOffset() - 1));
                fea_vec.addBinaryFeature("type_concat:" + left.getEntityType() + right.getEntityType());

                //Word based features
                int sen_offset = left.getSentenceOffset();
                int sen_start = document.getSentenceIndex(sen_offset);
                int sen_end = document.getSentenceIndex(sen_offset + 1) - 1;

                int leftstart = left.getStartOffset();
                int leftend = left.getEndOffset();
                int rightstart = right.getStartOffset();
                int rightend = right.getEndOffset();

                String E1_before = "_none_";
                String E2_after = "_none_";

                if (leftstart > sen_start) {
                    E1_before = lemmas.get(leftstart - 1);
                    if (NumberUtils.isNumber(E1_before))
                        E1_before = "_digit_";
                }
                if (rightend < sen_end) {
                    E2_after = lemmas.get(rightend);
                    if (NumberUtils.isNumber(E2_after))
                        E2_after = "_digit_";
                }

                fea_vec.addBinaryFeature("E1_before:" + E1_before);
                fea_vec.addBinaryFeature("E2_after:" + E2_after);

                for (int i = leftend; i < rightstart; i++) {
                    String word = lemmas.get(i);
                    if (NumberUtils.isNumber(word))
                        word = "_digit_";
                    fea_vec.addBinaryFeature("word:" + word);
                }


                //Add label
                Pair<EntityMention, EntityMention> pair_key = new Pair<>(left, right);
                String relation = "NO_RELATION";
                if (gold_relation.containsKey(pair_key)) {
                    relation = gold_relation.get(pair_key).getType();
                }

                fea_vec.addlabelCount(relation);
                extracted_data.add(fea_vec);

            }

        }

        System.out.println("Extract "+extracted_data.size()+ "instances");

        int features_count=extracted_data.get(0).getFeatureCount();
        int labels_count=extracted_data.get(0).getLabelCount();

        FeatureVectorSet data_set=new FeatureVectorSet(extracted_data,features_count,labels_count);

        data_set.writeToFile();




        /*
        //output binary features and labels

        PrintWriter writer=new PrintWriter("RE_data");

        int features_count=extracted_data.get(0).getFeatureCount();

        //information on the data
        //features count
        writer.print(features_count+",");
        //trainning set size
        writer.print((extracted_data.size()*4)/5+",");
        //testing set size
        writer.print((extracted_data.size()*1)/5+"\n");


        //features and labels
        for(FeatureVector f:extracted_data){
            List<Integer> vec = f.getFeatures();
            for(int i=0;i<vec.size();i++)
                writer.print(vec.get(i)+",");
            for(int i=0;i<features_count-vec.size();i++)
                writer.print("0,");
            writer.print(f.getLabelString()+"\n");
        }
        writer.close();
        */



    }

}
