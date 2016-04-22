package data;


import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import experiments.NaiveBayes;
import experiments.REFeatures;
import learn.FeatureVector;

import java.util.*;


/**
 * This class contain all useful information for global inference
 * GISentence stand for Global Inference Sentence
 * Created by sdq on 4/13/16.
 */


public class GISentence {


    public ACEAnnotation document;
    public List<String> sentence;

    public List<String> postags;
    public List<List<EntityMention>> corefgroup;

    public List<EntityMention> mentions;
    public List<Relation> relations;
    public Map<Pair<EntityMention,EntityMention>, Relation> relationmap;

    public List<String> lemmas;

    public GISentence(){

    }

    /**
     * this method is equivalent to a GIsentence class constructor, it break a list of document into GIsentence instances
     */
    public static List<GISentence> BreakDocumentIntoSentence(List<ACEAnnotation> test_set, int mode){

        // mode=0 turn off grouping

        //Turn ACEAnnotations into sentences
        List<GISentence> test_sentence = new ArrayList<>();

        for(ACEAnnotation document: test_set){

            //get all information relating to the document
            int sentences_count = document.getNumberOfSentences();

            List<List<String>> sentences = document.getSentences();
            List<List<String>> lemmas = document.getLemmasBySentence();
            List<List<String>> postag = document.getPOSTagsBySentence();

            Map<Pair<EntityMention, EntityMention>, CoreferenceEdge> gold_coreferences = document.getGoldCoreferenceEdgesByEntities();
            List<EntityMention> gold_entitymention = document.getGoldEntityMentions();
            List<List<EntityMention>> entity_bysentence = document.splitMentionBySentence(gold_entitymention);
            List<List<Relation>> pair_by_sentence = getRelationBySentence(entity_bysentence);

            //iterate through all sentences
            for(int i=0; i<sentences_count; i++){

                //build GISentence one at a time
                GISentence sentence_instance = new GISentence();
                sentence_instance.document=document;
                sentence_instance.lemmas=lemmas.get(i);
                sentence_instance.sentence=sentences.get(i);
                sentence_instance.mentions=entity_bysentence.get(i);
                sentence_instance.relations=pair_by_sentence.get(i);
                sentence_instance.postags=postag.get(i);
                sentence_instance.corefgroup=new ArrayList<>();
                sentence_instance.relationmap=new HashMap<>();


                int coref_count=0;
                for(Relation r: sentence_instance.relations){

                    EntityMention e1 = r.getArg1();
                    EntityMention e2 = r.getArg2();

                    Pair<EntityMention, EntityMention> p = new Pair (e1, e2);
                    Pair<EntityMention, EntityMention> p2 = new Pair (e2, e1);

                    sentence_instance.relationmap.put(p,r);
                    sentence_instance.relationmap.put(p2,r);

                    //assign coreference group index
                    boolean condition = gold_coreferences.containsKey(p) || gold_coreferences.containsKey(p2);
                    if(mode == 0)
                        condition = false;
                    if(condition){

                        if(e1.corefGroupIndex==-1 && e2.corefGroupIndex==-1){
                            e1.corefGroupIndex=coref_count;
                            e2.corefGroupIndex=coref_count;
                            coref_count++;
                        }

                        else if(e1.corefGroupIndex==-1){
                            e1.corefGroupIndex = e2.corefGroupIndex;
                        }
                        else if(e2.corefGroupIndex==-1){
                            e2.corefGroupIndex = e1.corefGroupIndex;
                        }

                    }
                    else{
                        if(e1.corefGroupIndex==-1){
                            e1.corefGroupIndex=coref_count;
                            coref_count++;
                        }
                        if(e2.corefGroupIndex==-1){
                            e2.corefGroupIndex=coref_count;
                            coref_count++;
                        }
                    }
                }

                //group Entity into Coreference group
                for(int j=0; j<coref_count; j++){
                    sentence_instance.corefgroup.add(new ArrayList<EntityMention>());
                }

                for(EntityMention m: sentence_instance.mentions){
                    m.sentence = sentence_instance;
                    int index=m.corefGroupIndex;
                    if(index==-1){
                        index=0;
                        sentence_instance.corefgroup.add(new ArrayList<EntityMention>());
                    }
                    sentence_instance.corefgroup.get(index).add(m);
                }

                test_sentence.add(sentence_instance);
            }

        }

        return test_sentence;
    }




    /**
     * get all possible relations from a document
     */
    public static List<List<Relation>> getRelationBySentence(List<List<EntityMention>> MentionsBySentence){

        List<List<Relation>> output=new ArrayList<>();
        for(int i=0;i<MentionsBySentence.size();i++){

            List<Relation> possible_pair_in_sentence=new ArrayList<>();
            List<EntityMention> mention_in_sentence=MentionsBySentence.get(i);

            //make all possible combination without duplication
            int length=mention_in_sentence.size();
            for(int j=0;j<length-1;j++){
                for(int k=j+1;k<length;k++) {
                    possible_pair_in_sentence.add(new Relation("NO_RELATION", mention_in_sentence.get(j),mention_in_sentence.get(k)));
                }
            }

            output.add(possible_pair_in_sentence);
        }

        return output;
    }

    /**
     * print all relevant information of sentence instances from a list
     */
    public static void printGiInformation(List<GISentence> gi_sentences){

        System.out.println();

        for(GISentence gs: gi_sentences){

                if (gs.corefgroup.size() != gs.mentions.size()) {

                    for (Relation r : gs.relations) {
                        if (r.type_num != 0 && r.type_num != r.pred_num) {
                            if (r.pred_vector != null) {

                                ACEAnnotation.printSentence(gs.sentence);
                                System.out.print(r.getArg1().getExtent() + " ");
                                System.out.println(r.getArg2().getExtent());
                                System.out.println("1st: " + Relation.stringList.get(r.pred_vector.get(0)) + " ");
                                System.out.println("2nd: " + Relation.stringList.get(r.pred_vector.get(1)) + " ");
                                System.out.println("3rd: " + Relation.stringList.get(r.pred_vector.get(2)) + " ");
                                System.out.println("Truth: " + r.type + "\n");

                            }
                        }
                    }

                }

        }



    }

    /**
     * when mode = 0, original Naive Bayes, when mode = 1, Naive Bayes + no_relation in same coreference group
     */
    public void assignPrediction(NaiveBayes clf, int mode){

        //assign prediction without coref constraints
        for(Relation r: this.relations){

            EntityMention e1 = r.getArg1();
            EntityMention e2 = r.getArg2();

            FeatureVector f = new FeatureVector();
            REFeatures.FeatureForOneInstance(e1, e2, f);
            List<Integer> pred_vec= clf.predict(f);
            int prediction = pred_vec.get(0);
            r.SetPrediction(prediction);
            r.pred_vector=pred_vec;

        }

        if(mode==1) {
            List<List<EntityMention>> group = this.corefgroup;
            //set NO_RELATION within the same corefgroup
            for (int i = 0; i < group.size(); i++) {
                List<EntityMention> g = group.get(i);
                if (g.size() > 1) {
                    for (int ii = 0; ii < g.size(); ii++) {
                        for (int jj = ii + 1; jj < g.size(); jj++) {
                            //0 standfor NO_RELATION
                            Relation r = this.relationmap.get(new Pair(g.get(ii), g.get(jj)));
                            r.SetPrediction(0);
                        }
                    }
                }
            }
        }

    }

    /**
     * predict relation by forcing same coreference group predict the same relations
     */
    public void assignPredictionWithCorefConstraint(NaiveBayes clf){

        List<List<EntityMention>> group=this.corefgroup;

        //iterate through all pair of group
        for(int i=0; i<group.size(); i++){
            for(int j=i+1; j<group.size();j++){

                //predict relation between two group
                List<EntityMention> g1 = group.get(i);
                List<EntityMention> g2 = group.get(j);
                List<Integer> pred_vector = NaiveBayes.RelationbetweenCorefGroup(g1,g2,clf);
                int prediction = pred_vector.get(0);

                //set relation for between two group
                for(int ii=0; ii<g1.size(); ii++){
                    for(int jj=0; jj<g2.size(); jj++){
                        Relation r = this.relationmap.get(new Pair(g1.get(ii), g2.get(jj)));
                        r.SetPrediction(prediction);
                        r.pred_vector=pred_vector;
                    }
                }
            }
        }

        //set NO_RELATION within the same corefgroup
        for(int i=0; i<group.size(); i++) {
            List<EntityMention> g = group.get(i);
            if (g.size() > 1) {
                for (int ii = 0; ii < g.size(); ii++) {
                    for (int jj = ii+1; jj < g.size(); jj++) {
                        //0 standfor NO_RELATION
                        Relation r = this.relationmap.get(new Pair(g.get(ii), g.get(jj)));
                        r.SetPrediction(0);
                    }
                }
            }
        }


    }


    /**
     * check the coresponding gold relations and assign true label on it
     */
    public static void assignTrueLabel(GISentence g){

        Map<Pair<EntityMention, EntityMention>, Relation> map = g.document.getGoldRelationsByArgs();

        for(Relation r: g.relations){

            EntityMention e1=r.getArg1();
            EntityMention e2=r.getArg2();

            Pair<EntityMention,EntityMention> p1 = new Pair(e1, e2);
            Pair<EntityMention,EntityMention> p2 = new Pair(e2, e1);

            Relation relation = map.get(p1);
            if(relation==null) {
                relation = map.get(p2);
            }

            if(relation!=null) {
                r.SetRelation(relation.type_num);
            }
        }
    }


    /**
     * add relations based on coreference
     */
    public static void IncrementRelationFromCoref(List<GISentence> sentences){

        for(GISentence g: sentences){

            //add relation in dictionary
            Map<Pair<EntityMention,EntityMention>, Relation> map = g.document.getGoldRelationsByArgs();

            for(Relation r: g.relations){

                EntityMention e1=r.getArg1();
                EntityMention e2=r.getArg2();

                Pair<EntityMention,EntityMention> p1 = new Pair(e1,e2);
                Pair<EntityMention,EntityMention> p2 = new Pair(e2,e1);

                Relation gold_relation=map.get(p1);
                if(gold_relation==null) {
                    gold_relation = map.get(p2);
                }

                if(gold_relation==null) {
                    continue;
                }

                List<EntityMention> g1 = g.corefgroup.get(e1.corefGroupIndex);
                List<EntityMention> g2 = g.corefgroup.get(e2.corefGroupIndex);

                //System.out.println(gold_relation.type_num);
                for (int i = 0; i < g1.size(); i++) {
                    for (int j = 0; j < g2.size(); j++) {
                        Pair<EntityMention, EntityMention> p = new Pair<>(g1.get(i), g2.get(j));
                        map.put(p, new Relation(gold_relation.type, p.getFirst(), p.getSecond()));
                    }
                }
            }



        }
    }

    public static void ResultSummary(List<GISentence> test_sentences){

        int labels_count = Relation.labels_count;
        int [] c_pick = new int [labels_count];
        int [] c_hit = new int [labels_count];
        int [] c_count = new int [labels_count];

        int tp=0;
        int tn=0;
        int fp=0;
        int fn=0;

        int hit=0;
        int count=0;


        for(GISentence g: test_sentences){
            for(Relation r: g.relations){

                if (r.pred_num == r.type_num) {

                    c_hit[r.type_num]++;
                    if(r.pred_num!=0) {
                        hit++;
                        tp++;
                    }
                    if(r.pred_num==0)
                        tn++;
                }
                else{
                    if(r.pred_num!=0)
                        fp++;
                    if(r.pred_num==0)
                        fn++;
                }

                c_pick[r.pred_num]++;
                c_count[r.type_num]++;
                if(r.type_num!=0)
                    count++;

            }
        }


        float acc = (float)(tp+tn)/(tp+tn+fp+fn);
        float precision = (float)tp/(tp+fp);
        float recall = (float) tp/(tp+fn);
        float true_acc = (float) hit/count;

        System.out.println("\naccuracy: "+ acc);
        System.out.println("true_accuracy: "+true_acc);
        System.out.println("precision: "+ precision);
        System.out.println("recall: "+ recall);
        System.out.println("F1 score: " + (precision+recall)/2);


        for(int i=0;i<labels_count;i++){

            System.out.print("Class "+i+" ");
            System.out.print("Count "+c_count[i]+" ");
            System.out.print("Pick "+c_pick[i]+" ");
            System.out.print("Hit "+c_hit[i]+" ");
            System.out.println((float)c_hit[i]/c_count[i]);

        }


    }




}
