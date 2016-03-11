package data;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.commondatastructure.AnnotatedText;

import java.util.*;

/**
 * This class wraps the information held in ACEDocument and makes it easier to use. ACEDocument contains
 * several pointless layers of wrappers that make doing anything a pain.
 *
 * Created by Colin Graber on 3/11/16.
 */
public class ACEAnnotation {

    // The following two lists hold all of the relation types/subtypes seen
    private static Set<String> relationTypes;

    static {
        relationTypes = new HashSet<String>();
    }


    private String id;
    private List<TextAnnotation> taList = new ArrayList<TextAnnotation>();

    // Each sentence is represented as a List of tokens - this is a list of those lists
    private List<List<String>> sentenceTokens = new ArrayList<List<String>>();
    private List<ACEEntity> entityList;
    private List<ACERelation> relationList;


    public ACEAnnotation(ACEDocument doc) {
        id = doc.aceAnnotation.id;


        // The TextAnnotations inside an ACEDocument are within a pointless wrapper - we remove those here
        for (AnnotatedText at: doc.taList) {
            taList.add(at.getTa());
        }

        // Since there may be multiple text annotations, each holding multiple sentences, we make accessing sentences
        // easier
        for (TextAnnotation ta: taList) {
            for (Sentence sentence: ta.sentences()) {
                sentenceTokens.add(Arrays.asList(sentence.getTokens()));
            }
        }


        // And now we pull all of the gold data out of the ACEDocumentAnnotation wrapper
        entityList = doc.aceAnnotation.entityList;
        relationList = doc.aceAnnotation.relationList;

        for (ACERelation relation: relationList) {
            relationTypes.add(relation.type);
        }

        //TODO: figure out how best to organize coreference/relation information
    }

    public int getNumberOfSentences() {
        return sentenceTokens.size();
    }

    public Iterator<List<String>> sentenceIterator() {
        return sentenceTokens.iterator();
    }


    /**
     *
     * @return All sentences in the document
     */
    public List<List<String>> getSentences() {
        return sentenceTokens;
    }

    /**
     * @param ind The sentence number within the document
     * @return The list of tokens for the given sentence, or null if the index is invalid
     */
    public List<String> getSentence(int ind) {
        if (ind >= sentenceTokens.size() || ind < 0) {
            return null;
        } else {
            return sentenceTokens.get(ind);
        }
    }

    public static Set<String> getRelationTypes() {
        return relationTypes;
    }

}
