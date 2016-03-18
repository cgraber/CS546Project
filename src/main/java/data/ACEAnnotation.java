package data;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntity;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACERelation;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.reader.util.EventConstants;
import utils.Consts;
import utils.Pipeline;

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
    private static Set<String> bioLabels;

    static {
        relationTypes = new HashSet<>();
        relationTypes.add(Consts.NO_REL);
        entityTypes = new HashSet<>();
        entitySubtypes = new HashSet<>();
    }


    private String id;
    private List<TextAnnotation> taList;

    // Each sentence is represented as a List of tokens - this is a list of those lists
    private List<List<String>> sentenceTokens = new ArrayList<>();
    private List<EntityMention> goldEntities = new ArrayList<>();
    private List<ACERelation> relationList;

    // Annotation info
    private List<List<String>> BIOencoding = null;

    public ACEAnnotation(ACEDocument doc) {
        id = doc.aceAnnotation.id;

        taList = AceFileProcessor.populateTextAnnotation(doc);

        // Since there may be multiple text annotations, each holding multiple sentences, we make accessing sentences
        // easier
        for (TextAnnotation ta: taList) {
            Pipeline.addAllViews(ta);
            for (Sentence sentence: ta.sentences()) {
                sentenceTokens.add(Arrays.asList(sentence.getTokens()));
            }
        }


        // And now we pull all of the gold data out of the ACEDocumentAnnotation wrapper
        relationList = doc.aceAnnotation.relationList;

        for (ACERelation relation: relationList) {
            relationTypes.add(relation.type);
        }
        System.out.println(sentenceTokens);
        for (ACEEntity entity: doc.aceAnnotation.entityList) {
            entityTypes.add(entity.type);
            entitySubtypes.add(entity.subtype);
            for (ACEEntityMention mention: entity.entityMentionList) {
                goldEntities.add(makeEntityMention(mention));
            }
        }

        //TODO: figure out how best to organize coreference/relation information

    }

    //NOTE: because of the (incorrect) tokenization, this introduces a bit of inaccuracy into the gold labels -
    // for the time being, we can't get around this.
    private EntityMention makeEntityMention(ACEEntityMention mention) {
        IntPair offsets = findTokenOffsets(mention.extentStart, mention.extentEnd);
        return new EntityMention(mention.type, offsets.getFirst(), offsets.getSecond(), this);
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

    /**
     *
     * @return A list of lists - each of these representing a sentence - of POS tags (representing the tag per
     *         word in the given sentence)
     */
    public List<List<String>> getPOSTags() {
        List<List<String>> result = new ArrayList<List<String>>();
        for (TextAnnotation ta: taList) {
            for (Sentence sentence: ta.sentences()) {
                List<String> posList = new ArrayList<>();
                result.add(posList);
                View posView = ta.getView(ViewNames.POS);
                for (int i = 0; i < sentence.getTokens().length; i++) {
                    posList.add(posView.getLabelsCoveringToken(i).get(0));
                }
            }
        }

        return result;
    }

    /**
     * This is the function that should be called by the NER system to add an entity to the test labels
     *
     * @param label The label of the entity
     * @param startOffset The index of the first token of the span containing the entity
     * @param endOffset The index of the last token + 1 (e.g. if the last token is #3, the value here should be 4)
     */
    public void addEntity(String label, int startOffset, int endOffset) {
        //TODO: Implement this!
    }

    public void addRelation(String label, EntityMention e1, EntityMention e2) {
        //TODO: Implement this!
    }

    public void addCoreferenceEdge(EntityMention e1, EntityMention e2) {
        //TODO: Implement this!
    }

    public void getGoldEntities() {
        //TODO: Implement this!
    }

    public void getGoldRelations() {
        //TODO: Implement this!
    }

    public void getGoldCoreferenceEdges() {
        //TODO: Implement this!
    }

    public List<String> getExtent(int start, int end) {
        int tokenCount = -1;
        List<String> result = new ArrayList<>();
        for (TextAnnotation ta: taList) {
            if (start > tokenCount + ta.getTokens().length) {
                tokenCount += ta.getTokens().length;
                continue;
            }
            for (int i = 0; i < ta.getTokens().length; i++) {
                tokenCount++;
                if (tokenCount < start) {
                    continue;
                } else if (tokenCount < end) {
                    result.add(ta.getTokens()[i]);
                } else {
                    break;
                }
            }
            if (tokenCount >= end) {
                break;
            }
        }
        return result;
    }

    /**
     * Searches through all of the text annotations to find the token offsets for a given mention
     * NOTE: due to incorrect tokenization, the mention boundaries may lie within a token; in that case, we "round"
     *
     * @param mentionStart The character offset for the start of the mention
     * @param mentionEnd The character offset for the end of the mention
     * @return an IntPair representing the start and end+1 indices of the mention.
     */
    private IntPair findTokenOffsets(int mentionStart, int mentionEnd )
    {
        int tokenStart = -1;
        int tokenEnd = -1;
        int tokenStartOffset = 0;
        int tokenEndOffset = 0;
        for (TextAnnotation ta: taList) {

            View tokenOffsetView = ta.getView(EventConstants.TOKEN_WITH_CHAR_OFFSET);


            for (Constituent t : tokenOffsetView.getConstituents()) {
                if (Integer.parseInt(t.getAttribute(EventConstants.CHAR_START)) <= mentionStart) {
                    tokenStart = t.getStartSpan();
                }
                if (tokenEnd == -1 && Integer.parseInt(t.getAttribute(EventConstants.CHAR_END)) >= mentionEnd) {
                    tokenEnd = t.getEndSpan();
                }
            }

            if (tokenStart >= 0 && tokenEnd >= 0) {
                return new IntPair(tokenStart + tokenStartOffset, tokenEnd+tokenEndOffset);
            } else {
                tokenStartOffset += ta.getTokens().length;
                tokenEndOffset += ta.getTokens().length;
            }
        }
        return null;
    }

    // Static methods - these are used to access global information relating to the dataset

    public static Set<String> getRelationTypes() {
        return relationTypes;
    }

    public static Set<String> getEntityTypes() {
        return entityTypes;
    }

    public static Set<String> getEntitySubtypes() {
        return entitySubtypes;
    }

    public static Set<String> getBIOLabels() {
        if (bioLabels != null) {
            return bioLabels;
        }
        bioLabels = new HashSet<String>();

        bioLabels.add(Consts.BIO_O);
        for (String type: getEntityTypes()) {
            bioLabels.add(Consts.BIO_B + type);
            bioLabels.add(Consts.BIO_I + type);
        }

        return bioLabels;
    }



}
