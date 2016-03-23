package experiments;

import data.ACEAnnotation;
import data.EntityMention;
import data.DataUtils;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.io.IOException;
import java.util.*;

/**
 * Created by sdq on 3/22/16.
 */
public class RelationExtraction {

    public static void main(String [] argv) throws IOException {

        //List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits("./ACE05_English");
        //ACEAnnotation.writeAlltoFile(splits);

        ACEAnnotation document=ACEAnnotation.readFileByID(0);
        List<EntityMention> gold_m=document.getGoldEntityMentions();

        //sort EntityMention by startOffset
        Collections.sort(gold_m, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention o1, EntityMention o2) {
                return o1.getStartOffset()-o2.getStartOffset();
            }
        });


        List<List<EntityMention>> gold_m_sentence=document.splitMentionBySentence(gold_m);
        List<Pair<EntityMention,EntityMention>> possible_pair=ACEAnnotation.getPossibleMentionPair(gold_m_sentence);




        for(Pair<EntityMention,EntityMention> p: possible_pair){

            System.out.println(p.getFirst().getExtent());
            System.out.println(p.getSecond().getExtent());
            int index=p.getFirst().getSentenceOffset();
            System.out.println(document.getSentence(index));




        }



    }

}
