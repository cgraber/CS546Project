package data;


import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sdq on 4/13/16.
 */
public class GISentence {

    public ACEAnnotation document;
    public List<String> sentence;
    public List<EntityMention> mentions;
    public List<Relation> relations;
    public List<String> lemmas;

    public GISentence(){


    }

    /*
    public ACEAnnotation getDocument(){ return document; }
    public List<String> getSentence() { return sentence; }
    public List<EntityMention> getMentions() { return mentions; }
    public List<Pair<EntityMention,EntityMention>> getRelations() { return relations; }
    public List<String> getLemmas(){ return lemmas; }
    */

    public static List<GISentence> BreakDocumentIntoSentence(List<ACEAnnotation> test_set){

        //Turn ACEAnnotations into sentences
        List<GISentence> test_sentence = new ArrayList<>();

        for(ACEAnnotation document: test_set){

            int sentences_count = document.getNumberOfSentences();

            List<List<String>> sentences = document.getSentences();
            List<List<String>> lemmas = document.getLemmasBySentence();

            List<EntityMention> gold_m = document.getGoldEntityMentions();
            List<List<EntityMention>> gold_m_sentence = document.splitMentionBySentence(gold_m);

            List<List<Relation>> pair_by_sentence = getRelationBySentence(gold_m_sentence);


            for(int i=0; i<sentences_count; i++){
                GISentence sentence_instance = new GISentence();
                sentence_instance.document=document;
                sentence_instance.lemmas=lemmas.get(i);
                sentence_instance.sentence=sentences.get(i);
                sentence_instance.mentions=gold_m_sentence.get(i);
                sentence_instance.relations=pair_by_sentence.get(i);
                test_sentence.add(sentence_instance);
            }

        }

        return test_sentence;
    }




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
