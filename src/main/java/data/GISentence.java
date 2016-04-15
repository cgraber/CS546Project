package data;


import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
    public List<String> lemmas;

    public GISentence(){

    }

    /**
     * this method is equivalent to a GIsentence class constructor, it break a list of document into GIsentence instances
     */
    public static List<GISentence> BreakDocumentIntoSentence(List<ACEAnnotation> test_set){

        //Turn ACEAnnotations into sentences
        List<GISentence> test_sentence = new ArrayList<>();

        for(ACEAnnotation document: test_set){

            int sentences_count = document.getNumberOfSentences();

            List<List<String>> sentences = document.getSentences();
            List<List<String>> lemmas = document.getLemmasBySentence();
            List<List<String>> postag = document.getPOSTagsBySentence();

            Map<Pair<EntityMention, EntityMention>, CoreferenceEdge> gold_coreferences = document.getGoldCoreferenceEdgesByEntities();

            List<EntityMention> gold_m = document.getGoldEntityMentions();
            List<List<EntityMention>> gold_m_sentence = document.splitMentionBySentence(gold_m);

            List<List<Relation>> pair_by_sentence = getRelationBySentence(gold_m_sentence);



            for(int i=0; i<sentences_count; i++){

                //build GISentence one at a time
                GISentence sentence_instance = new GISentence();
                sentence_instance.document=document;
                sentence_instance.lemmas=lemmas.get(i);
                sentence_instance.sentence=sentences.get(i);
                sentence_instance.mentions=gold_m_sentence.get(i);
                sentence_instance.relations=pair_by_sentence.get(i);
                sentence_instance.postags=postag.get(i);
                sentence_instance.corefgroup=new ArrayList<>();

                ACEAnnotation.printSentence(sentence_instance.sentence);
                System.out.println(sentence_instance.mentions);

                int coref_count=0;

                for(Relation r: sentence_instance.relations){

                    EntityMention e1 = r.getArg1();
                    EntityMention e2 = r.getArg2();

                    Pair<EntityMention, EntityMention> p = new Pair (e1, e2);
                    Pair<EntityMention, EntityMention> p2 = new Pair (e2, e1);

                    //assign coreference group index
                    if(gold_coreferences.containsKey(p) || gold_coreferences.containsKey(p2)){

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


                System.out.println("coref_count "+coref_count);

                //group Entity into Coreference group
                for(int j=0; j<coref_count; j++){
                    sentence_instance.corefgroup.add(new ArrayList<EntityMention>());
                }

                for(EntityMention m: sentence_instance.mentions){
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
                    possible_pair_in_sentence.add(new Relation("Unknown", mention_in_sentence.get(j),mention_in_sentence.get(k)));
                }
            }

            output.add(possible_pair_in_sentence);
        }

        return output;
    }


}
