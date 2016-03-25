package experiments;

import data.ACEAnnotation;
import data.EntityMention;
import data.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import static java.lang.Math.log;

/**
 * Created by sdq on 3/23/16.
 */
public class Test_Playground {

    public static void main(String [] argv) throws IOException {


        int annot=0;
        int nor=0;

        for (int i=0;i<324;i++) {

            ACEAnnotation document=ACEAnnotation.readFileByID(i);
            List<Relation> relation=document.getGoldRelations();
            Map<Pair<EntityMention,EntityMention>,Relation> arg_relation=document.getGoldRelationsByArgs();
            Pair<List<Relation>,List<Relation>> all=document.getAllPairsGoldRelations();

            annot+=all.getFirst().size();
            nor+=all.getSecond().size();

            /*
            for(Relation x : relation){
                EntityMention left=x.getArg1();
                EntityMention right=x.getArg2();
                if(left.getSentenceOffset()!=right.getSentenceOffset()){

                    System.out.println(left.getExtent());

                    System.out.println(right.getExtent());

                    System.out.println(document.getSentence(left.getSentenceOffset()));

                    System.out.println(document.getSentence(right.getSentenceOffset()));

                    System.out.print("\n");

                }




            }
            */


        }

        System.out.println(annot);
        System.out.println(nor);



    }

}
