package data;


import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.util.List;

/**
 * Created by sdq on 4/13/16.
 */
public class GISentence {

    public ACEAnnotation document;
    public List<String> sentence;
    public List<EntityMention> mentions;
    public List<Pair<EntityMention,EntityMention>> relations;
    public List<String> lemmas;

    public GISentence(){


    }


    public ACEAnnotation getDocument(){ return document; }
    public List<String> getSentence() { return sentence; }
    public List<EntityMention> getMentions() { return mentions; }
    public List<Pair<EntityMention,EntityMention>> getRelations() { return relations; }
    public List<String> getLemmas(){ return lemmas; }


}
