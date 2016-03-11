package data;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.commondatastructure.AnnotatedText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class wraps the information held in ACEDocument and makes it easier to use. ACEDocument contains
 * several pointless layers of wrappers that make doing anything a pain.
 *
 * Created by Colin Graber on 3/11/16.
 */
public class ACEAnnotation {

    // The following two lists hold all of the relation types/subtypes seen
    private static Set<String> relationTypes;
    private static Set<String> relationSubtypes;

    static {
        relationTypes = new HashSet<String>();
        relationSubtypes = new HashSet<String>();
    }


    private String id;
    private List<TextAnnotation> taList = new ArrayList<TextAnnotation>();
    private List<ACEEntity> entityList;
    private List<ACERelation> relationList;

    public ACEAnnotation(ACEDocument doc) {
        id = doc.aceAnnotation.id;


        // The TextAnnotations inside an ACEDocument are within a pointless wrapper - we remove those here
        for (AnnotatedText at: doc.taList) {
            taList.add(at.getTa());
        }

        // And now we pull all of the gold data out of the ACEDocumentAnnotation wrapper
        entityList = doc.aceAnnotation.entityList;
        relationList = doc.aceAnnotation.relationList;

        //TODO: figure out how best to organize coreference/relation information
    }

    public static Set<String> getRelationTypes() {
        return relationTypes;
    }

    public static Set<String> getRelationSubtypes() {
        return relationSubtypes;
    }
}
