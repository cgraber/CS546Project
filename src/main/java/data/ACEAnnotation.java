package data;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.reader.commondatastructure.AnnotatedText;
import edu.illinois.cs.cogcomp.reader.util.EventConstants;
import utils.Consts;

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
    private static Set<String> entityTypes;
    private static Set<String> entitySubtypes;

    static {
        relationTypes = new HashSet<>();
        entityTypes = new HashSet<>();
        entitySubtypes = new HashSet<>();
    }


    private String id;
    private List<TextAnnotation> taList;

    // Each sentence is represented as a List of tokens - this is a list of those lists
    private List<List<String>> sentenceTokens = new ArrayList<List<String>>();
    private List<ACEEntity> entityList;
    private List<ACERelation> relationList;

    // Annotation info
    private List<List<String>> BIOencoding = null;

    public ACEAnnotation(ACEDocument doc) {
        id = doc.aceAnnotation.id;

        taList = AceFileProcessor.populateTextAnnotation(doc);

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

        for (ACEEntity entity: entityList) {
            entityTypes.add(entity.type);
            entitySubtypes.add(entity.subtype);
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

    /**
     * NOTE: Using BIO assumes that the entity spans are non-overlapping, which isn't true in this entire dataset.
     * @return The Encodings, represented as a list of encodings per sentence
     */
    public List<List<String>> getGoldBIOEncoding() {
        if (BIOencoding != null) {
            return BIOencoding;
        }
        BIOencoding = new ArrayList<List<String>>();
        System.out.println(sentenceTokens);
        for (TextAnnotation ta: taList) {
            System.out.println(ta);
            if (!ta.hasView(EventConstants.NER_ACE_COARSE)) {
                for (Sentence sentence: ta.sentences()) {
                    List<String> labelList = new ArrayList<>();
                    BIOencoding.add(labelList);
                    for (String token: sentence.getTokens()) {
                        labelList.add(Consts.BIO_O);
                    }
                }
            }
            else {
                View nerView = ta.getView(EventConstants.NER_ACE_COARSE);
                int tokenInd = 0;

                List<Constituent> labels = nerView.getConstituents();
                System.out.println(labels);
                Iterator<Constituent> labelItr = labels.iterator();
                Constituent currentLabel = labelItr.next();
                for (Sentence sentence : ta.sentences()) {
                    List<String> labelList = new ArrayList<>();
                    BIOencoding.add(labelList);
                    for (String token : sentence.getTokens()) {
                        if (currentLabel != null && tokenInd == currentLabel.getEndSpan()) {
                            //TODO: should we go for smallest spans or largest?
                            while (currentLabel != null && currentLabel.getEndSpan() <= tokenInd) {
                                currentLabel = labelItr.hasNext() ? labelItr.next() : null;
                            }
                        }
                        if (currentLabel == null || tokenInd < currentLabel.getStartSpan()) {
                            labelList.add(Consts.BIO_O);
                        } else if (!currentLabel.doesConstituentCover(tokenInd)) {
                            System.out.println(tokenInd);
                            System.out.println(currentLabel.getSpan());
                            throw new RuntimeException("BIO ERROR");
                        } else if (tokenInd == currentLabel.getStartSpan()) {
                            labelList.add(Consts.BIO_B + currentLabel.getLabel());
                        } else if (tokenInd > currentLabel.getStartSpan() && tokenInd < currentLabel.getEndSpan()) {
                            labelList.add(Consts.BIO_I + currentLabel.getLabel());
                        }

                        tokenInd++;
                    }
                }
            }
        }
        System.out.println(BIOencoding);
        return BIOencoding;
    }


    public static Set<String> getRelationTypes() {
        return relationTypes;
    }

    public static Set<String> getEntityTypes() {
        return entityTypes;
    }

    public static Set<String> getEntitySubtypes() {
        return entitySubtypes;
    }

}
