package experiments;

import data.*;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import learn.FeatureVector;
import org.apache.commons.lang.math.NumberUtils;

import java.io.IOException;
import java.util.*;
import java.lang.*;

/**
 * Created by sdq on 3/22/16.
 */

public class REFeatures {

    public static void main(String [] argv) throws IOException {

    }

    public static List<FeatureVector> generateFeatures() throws IOException{
        List<ACEAnnotation> collection = ACEAnnotation.readAllFromFileFlat();
        return generateFeatures(collection, 0.97f);
    }

    /**
     * this method generate Features all possible pair of Entity from a collection of document.
     * when mode = "train", this method will throw away 95% of the negative examples to achieve a more balanced data set.
     * the features for two EntityMentions (e1, e2) are as follow:
     *
     * the type of (e1.type), (e2.type) and the concatenation of the type(e1.type, e2.type)
     * the lemmas of the last word in both e1.mention and e2.mention
     * the lemmas of the word before e1.mention and the word after e2.mention
     * all the pos tags between e1.mention and e2.mention
     * --------------
     * the features that are available but not included in NaiveBayes Classifier:
     *
     * the lemmas of the word after e1.mention and the word before e2.mention
     * all the lemmas between e1.mention and e2.mention
     */

    public static List<FeatureVector> generateFeatures(List<ACEAnnotation> collection, float no_relation_block_rate) {


        //features and labels for all data
        List<FeatureVector> extracted_data=new ArrayList<>();

        //process one document at a time
        for(ACEAnnotation document: collection) {

            List<EntityMention> gold_m = document.getGoldEntityMentions();

            //get all possible relation from this document (including sorting)
            List<List<EntityMention>> gold_m_sentence = document.splitMentionBySentence(gold_m);
            List<Relation> possible_pair = ACEAnnotation.getPossibleMentionPair(gold_m_sentence);

            //get gold relations
            Map<Pair<EntityMention, EntityMention>, Relation> gold_relation = document.getGoldRelationsByArgs();

            //features builder on all possible relation
            for (Relation p : possible_pair) {

                //setup vector for storing features
                FeatureVector fea_vec = new FeatureVector();

                //Entity based features
                EntityMention left = p.getArg1();
                EntityMention right = p.getArg2();

                //Add label
                Pair<EntityMention,EntityMention> p1=new Pair<>(left,right);
                Pair<EntityMention, EntityMention> p2 = new Pair<>(right, left);


                //If gold_relation doesn't contain such pair, we take it as NO_RELATION
                String relation = "NO_RELATION";
                if (gold_relation.containsKey(p1)){
                    relation = gold_relation.get(p1).getType();
                }
                else if(gold_relation.containsKey(p2)){
                    relation = gold_relation.get(p2).getType();
                }

                //get rid of 95% of No-Relation
                if(relation.equals("NO_RELATION")){
                    double random= Math.random();
                    if(random < no_relation_block_rate)
                        continue;
                }

                fea_vec.addLabel(relation);
                extracted_data.add(fea_vec);
                REFeatures.FeatureForOneInstance(left, right, fea_vec);

            }

        }

        return extracted_data;

    }

    public static void FeatureForOneInstance(EntityMention p1, EntityMention p2, FeatureVector fea_vec){


        //make sure the order of entity is correct
        EntityMention left = null;
        EntityMention right = null;
        if(p1.extentStartOffset <= p2.extentStartOffset) {
            left = p1;
            right =p2;
        }
        else{
            left = p2;
            right = p1;
        }


        ACEAnnotation document = left.annotation;
        List<String> lemmas = document.getLemmas();
        List<String> pos_tags = document.getPOSTags();

        fea_vec.addBinaryFeature("E1_E_type:" + left.getEntityType());
        fea_vec.addBinaryFeature("E2_E_type:" + right.getEntityType());
        fea_vec.addBinaryFeature("type_concat:" + left.getEntityType() + right.getEntityType());

        fea_vec.addBinaryFeature("E1_head:" + lemmas.get(left.getExtentEndOffset() - 1));
        fea_vec.addBinaryFeature("E2_head:" + lemmas.get(right.getExtentEndOffset() - 1));


        //Word based features
        int sen_offset = left.getSentenceOffset();
        int sen_start = document.getSentenceIndex(sen_offset);
        int sen_end = document.getSentenceIndex(sen_offset + 1) - 1;

        int leftstart = left.getExtentStartOffset();
        int leftend = left.getExtentEndOffset();
        int rightstart = right.getExtentStartOffset();
        int rightend = right.getExtentEndOffset();

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

        //Syntactic features
        for(int i=leftend;i<rightstart;i++){
            String tag=pos_tags.get(i);
            //String word=lemmas.get(i);
            fea_vec.addBinaryFeature("Pos:" + tag);
            //fea_vec.addBinaryFeature("Word: "+ word);
        }

    }


}
