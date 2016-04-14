package data;

import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.aceReader.annotationStructure.*;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import utils.Consts;
import utils.Pipeline;

import java.io.*;
import java.util.*;

/**
 * This class wraps the information held in ACEDocument and makes it easier to use. ACEDocument contains
 * several pointless layers of wrappers that make doing anything a pain.
 *
 * Created by Colin Graber on 3/11/16.
 */
public class ACEAnnotation implements Serializable {

    private static final long serialVersionUID = 1L;
    private static TextAnnotationBuilder taBuilder;


    // The following two lists hold all of the relation types/subtypes seen
    private static Set<String> relationTypes;
    private static Set<String> entityTypes;
    private static Set<String> entitySubtypes;
    private static Set<String> mentionTypes;
    private static Set<String> bioLabels;

    static {
        relationTypes = new HashSet<>();
        relationTypes.add(Consts.NO_REL);
        entityTypes = new HashSet<>();
        mentionTypes = new HashSet<>();
        entitySubtypes = new HashSet<>();

        taBuilder = new TokenizerTextAnnotationBuilder(new IllinoisTokenizer());
    }


    private String id;
    private TextAnnotation ta;

    // Each sentence is represented as a List of tokens - this is a list of those lists
    private List<List<String>> sentenceTokens = new ArrayList<>();

    //This list simply contains all of the tokens in a flat list - useful when you don't care about sentence boundaries
    private List<String> tokens = new ArrayList<>();
    private List<EntityMention> goldEntityMentions = new ArrayList<>();
    private Map<String,EntityMention> goldEntityMentionsByID = new HashMap<>();
    private Map<IntPair,EntityMention> goldEntityMentionsByHeadSpan = new HashMap<>();

    private List<EntityMention> testEntityMentions = new ArrayList<>();
    private Map<IntPair,EntityMention> testEntityMentionsBySpan = new HashMap<>();
    private List<Relation> goldRelations = new ArrayList<>();
    private Map<Pair<EntityMention,EntityMention>,Relation> goldRelationsByArgs = new HashMap<>();
    private List<Relation> testRelations = new ArrayList<>();

    private List<CoreferenceEdge> goldCoreferenceEdges = new ArrayList<>();
    private Map<Pair<EntityMention,EntityMention>,CoreferenceEdge> goldCoreferenceEdgesByEntities = new HashMap<>();
    private List<CoreferenceEdge> testCoreferenceEdges = new ArrayList<>();
    private List<ACERelation> relationList;
    private List<Integer> sentenceIndex = new ArrayList<>();

    // Annotation info
    private List<List<String>> BIOencoding = null;

    public ACEAnnotation(ACEDocument doc) {
        id = doc.aceAnnotation.id;

        ta = taBuilder.createTextAnnotation(null, id, doc.contentRemovingTags);
        int count=0;
        Pipeline.addAllViews(ta);
        for (Sentence sentence: ta.sentences()) {
            List<String> sentenceArray=Arrays.asList(sentence.getTokens());
            sentenceTokens.add(sentenceArray);
            tokens.addAll(sentenceArray);
            sentenceIndex.add(count);
            count+=sentenceArray.size();
        }
        sentenceIndex.add(count);

        // And now we pull all of the gold data out of the ACEDocumentAnnotation wrapper
        relationList = doc.aceAnnotation.relationList;


        for (ACEEntity entity: doc.aceAnnotation.entityList) {
            entityTypes.add(entity.type);
            entitySubtypes.add(entity.subtype);
            List<EntityMention> coreferentEntities = new ArrayList<>();
            for (ACEEntityMention mention: entity.entityMentionList) {
                mentionTypes.add(mention.type);
                EntityMention e = makeEntityMention(mention, entity.type);
                goldEntityMentions.add(e);
                coreferentEntities.add(e);
                goldEntityMentionsByID.put(mention.id, e);
                goldEntityMentionsByHeadSpan.put(new IntPair(e.getHeadStartOffset(), e.getHeadEndOffset()), e);
                //System.out.println("\t"+mention.id+", "+mention.extent+", "+mention.type+", "+mention.ldcType);
            }
            // Add all pairs of coreference edges
            for (int i = 0; i < coreferentEntities.size(); i++) {
                for (int j = i+1; j < coreferentEntities.size(); j++) {
                    EntityMention e1 = coreferentEntities.get(i);
                    EntityMention e2 = coreferentEntities.get(j);
                    CoreferenceEdge edge = new CoreferenceEdge(e1, e2);
                    goldCoreferenceEdges.add(edge);
                    goldCoreferenceEdgesByEntities.put(new Pair<>(e1, e2), edge);
                }
            }
        }
        Collections.sort(goldEntityMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention e1, EntityMention e2) {
                if (e1.getExtentStartOffset() < e2.getExtentStartOffset()) {
                    return -1;
                } else if (e2.getExtentStartOffset() < e1.getExtentStartOffset()) {
                    return 1;
                } else if (e1.getExtentEndOffset() < e2.getExtentEndOffset()) {
                    return -1;
                } else if (e2.getExtentEndOffset() < e1.getExtentEndOffset()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        Collections.sort(goldEntityMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention e1, EntityMention e2) {
                if (e1.getExtentStartOffset() < e2.getExtentStartOffset()) {
                    return -1;
                } else if (e2.getExtentStartOffset() < e1.getExtentStartOffset()) {
                    return 1;
                } else if (e1.getExtentEndOffset() < e2.getExtentEndOffset()) {
                    return -1;
                } else if (e2.getExtentEndOffset() < e1.getExtentEndOffset()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        for (ACERelation relation: relationList) {
            relationTypes.add(relation.type);
            for (ACERelationMention rm : relation.relationMentionList) {
                EntityMention e1 = null;
                EntityMention e2 = null;
                for (ACERelationArgumentMention ram: rm.relationArgumentMentionList) {
                    if (ram.role.equals(Consts.ARG_1)) {
                        //System.out.println("\t\tArg_1: "+ram.id+", "+ram.argStr);
                        e1 = goldEntityMentionsByID.get(ram.id);
                    } else if (ram.role.equals(Consts.ARG_2)) {
                        //System.out.println("\t\tArg_2: "+ram.id+", "+ram.argStr);
                        e2 = goldEntityMentionsByID.get(ram.id);
                    }
                }
                Relation rel = new Relation(relation.type, e1, e2);
                goldRelations.add(rel);
                goldRelationsByArgs.put(new Pair<>(e1, e2), rel);
            }
        }
    }

    private int FindSentenceIndex(int start){

        for(int i=0;i<sentenceIndex.size()-1;i++){
            int index=sentenceIndex.get(i);
            int index2=sentenceIndex.get(i+1);
            if(index<=start && start<index2)
                return i;
        }
        return -1;

    }


    //NOTE: because of the (incorrect) tokenization, this introduces a bit of inaccuracy into the gold labels -
    // for the time being, we can't get around this.
    private EntityMention makeEntityMention(ACEEntityMention mention, String type) {
        IntPair extentOffsets = findTokenOffsets(mention.extentStart, mention.extentEnd);
        IntPair headOffsets = findTokenOffsets(mention.headStart, mention.headEnd);
        if (headOffsets.getFirst() >= headOffsets.getSecond()) {
            System.err.println("PROBLEM WITH SPANS");
            System.err.println("ORIGINAL: ("+mention.headStart +", "+mention.headEnd+")");
            System.err.println("FOUND: ("+headOffsets.getFirst()+", "+headOffsets.getSecond()+")");
            System.err.println(id);
            System.exit(1);
        }

        int sentenceOffset=FindSentenceIndex(extentOffsets.getFirst());
        return new EntityMention(type, mention.type, extentOffsets.getFirst(), extentOffsets.getSecond(), headOffsets.getFirst(), headOffsets.getSecond(), sentenceOffset, this);
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
    public int getSentenceIndex(int offset){ return sentenceIndex.get(offset); }


    public List<String> getTokens() {
        List<String> result = new ArrayList<>();
        for (List<String> sentence: sentenceTokens) {
            result.addAll(sentence);
        }
        return result;
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
     * @param ind The token number within the document
     * @return The requested token, or null if the index is invalid
     */
    public String getToken(int ind) {
        if (ind >= tokens.size() || ind < 0) {
            return null;
        } else {
            return tokens.get(ind);
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
        Collections.sort(goldEntityMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention e1, EntityMention e2) {
                return e1.getHeadStartOffset() - e2.getHeadStartOffset();
            }
        });

        for (EntityMention e: goldEntityMentions) {
            //System.out.println("("+e.getHeadStartOffset()+", "+e.getHeadEndOffset()+")");
        }
        Iterator<EntityMention> labelItr = goldEntityMentions.iterator();
        EntityMention currentLabel = labelItr.next();
        int tokenInd = 0;
        for (List<String> sentence: sentenceTokens) {
            List<String> labelList = new ArrayList<>();
            BIOencoding.add(labelList);
            for (String word: sentence) {
                if (currentLabel != null && tokenInd == currentLabel.getHeadEndOffset()) {
                    while (currentLabel != null && currentLabel.getHeadStartOffset() < tokenInd) {
                        currentLabel = labelItr.hasNext() ? labelItr.next() : null;
                    }
                }
                if (currentLabel == null || tokenInd < currentLabel.getHeadStartOffset()) {
                    labelList.add(Consts.BIO_O);
                } else if (tokenInd == currentLabel.getHeadStartOffset()) {
                    labelList.add(Consts.BIO_B + currentLabel.getEntityType());
                } else if (tokenInd > currentLabel.getHeadStartOffset() && tokenInd < currentLabel.getHeadEndOffset()) {
                    labelList.add(Consts.BIO_I + currentLabel.getEntityType());
                } else {
                    System.err.println("PROBLEM - current ind is "+tokenInd + ", offset is "+currentLabel.getHeadStartOffset());
                }
                tokenInd++;
            }
        }

        return BIOencoding;
    }

    /**
     *
     * @return A list of lists - each of these representing a sentence - of POS tags (representing the tag per
     *         word in the given sentence)
     */
    public List<List<String>> getPOSTagsBySentence() {
        List<List<String>> result = new ArrayList<List<String>>();

        int tokenInd = 0;
        for (Sentence sentence: ta.sentences()) {
            List<String> posList = new ArrayList<>();
            result.add(posList);
            View posView = ta.getView(ViewNames.POS);
            for (int i = 0; i < sentence.getTokens().length; i++) {
                posList.add(posView.getLabelsCoveringToken(tokenInd++).get(0));
            }
        }


        return result;
    }

    public List<String> getPOSTags() {
        List<String> result = new ArrayList<>();

        int tokenInd = 0;
        for (Sentence sentence: ta.sentences()) {
            View posView = ta.getView(ViewNames.POS);
            for (int i = 0; i < sentence.getTokens().length; i++) {
                result.add(posView.getLabelsCoveringToken(tokenInd++).get(0));
            }
        }

        return result;
    }

    /**
     *
     * @return A list of lists - each of these representing a sentence - of lemmas (representing the lemma per
     *         word in the given sentence)
     */
    public List<List<String>> getLemmasBySentence() {
        List<List<String>> result = new ArrayList<>();

        int tokenInd = 0;
        for (Sentence sentence: ta.sentences()) {
            List<String> lemmaList = new ArrayList<>();
            result.add(lemmaList);
            View lemmaView = ta.getView(ViewNames.LEMMA);
            for (int i = 0; i < sentence.getTokens().length; i++) {
                lemmaList.add(lemmaView.getLabelsCoveringToken(tokenInd++).get(0));
            }
        }


        return result;
    }

    public List<String> getLemmas() {
        List<String> result = new ArrayList<>();

        int tokenInd = 0;
        for (Sentence sentence: ta.sentences()) {
            View posView = ta.getView(ViewNames.LEMMA);
            for (int i = 0; i < sentence.getTokens().length; i++) {
                result.add(posView.getLabelsCoveringToken(tokenInd++).get(0));
            }
        }

        return result;
    }

    /**
     * This is the function that should be called by the NER system to add an entity mention to the test labels
     *
     * @param type The type of the entity
     * @param extentStartOffset The index of the first token of the span containing the entity
     * @param extentEndOffset The index of the last token + 1 (e.g. if the last token is #3, the value here should be 4)
     */
    public void addEntityMention(String type, int extentStartOffset, int extentEndOffset, int headStartOffset, int headEndOffset) {
        EntityMention e = new EntityMention(type, null, extentStartOffset, extentEndOffset, headStartOffset, headEndOffset, FindSentenceIndex(extentStartOffset), this);
        testEntityMentions.add(e);
        testEntityMentionsBySpan.put(new IntPair(extentStartOffset, extentEndOffset), e);
    }

    /**
     * This is the function that should be called by the relation extraction system to add a relation to the test labels
     * @param type The type of the relation
     * @param e1 The entity mention that takes the role of ARG-1 in the relation
     * @param e2 The entity mention that takes the role of ARG-2 in the relation
     */
    public void addRelation(String type, EntityMention e1, EntityMention e2) {
        testRelations.add(new Relation(type, e1, e2));
    }

    public void addCoreferenceEdge(EntityMention e1, EntityMention e2) {
        testCoreferenceEdges.add(new CoreferenceEdge(e1, e2));
    }

    public List<EntityMention> getGoldEntityMentions() {
        return goldEntityMentions;
    }

    public List<EntityMention> getTestEntityMentions() {
        return testEntityMentions;
    }



    public static List<Pair<EntityMention,EntityMention>> getPossibleMentionPair(List<List<EntityMention>> MentionsBySentence){

        List<Pair<EntityMention, EntityMention>> possible_pair=new ArrayList<>();
        for(int i=0;i<MentionsBySentence.size();i++){
            List<EntityMention> mention_in_sentence=MentionsBySentence.get(i);

            //make all possible combination without duplication
            int length=mention_in_sentence.size();
            for(int j=0;j<length-1;j++){
                for(int k=j+1;k<length;k++) {
                    possible_pair.add(new Pair<>(mention_in_sentence.get(j),mention_in_sentence.get(k)));
                }
            }
        }

        return possible_pair;

    }

    public static List<GISentence> BreakDocumentIntoSentence(List<ACEAnnotation> test_set){

        //Turn ACEAnnotations into sentences
        List<GISentence> test_sentence = new ArrayList<>();

        for(ACEAnnotation document: test_set){

            int sentences_count = document.getNumberOfSentences();

            List<List<String>> sentences = document.getSentences();
            List<List<String>> lemmas = document.getLemmasBySentence();

            List<EntityMention> gold_m = document.getGoldEntityMentions();

            List<List<EntityMention>> gold_m_sentence = document.splitMentionBySentence(gold_m);
            List<List<Pair<EntityMention, EntityMention>>> pair_by_sentence = ACEAnnotation.getMentionPairBySentence(gold_m_sentence);

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

    public static List<List<Pair<EntityMention,EntityMention>>> getMentionPairBySentence(List<List<EntityMention>> MentionsBySentence){

        List<List<Pair<EntityMention,EntityMention>>> output=new ArrayList<>();

        for(int i=0;i<MentionsBySentence.size();i++){

            List<Pair<EntityMention, EntityMention>> possible_pair_in_sentence=new ArrayList<>();
            List<EntityMention> mention_in_sentence=MentionsBySentence.get(i);

            //make all possible combination without duplication
            int length=mention_in_sentence.size();
            for(int j=0;j<length-1;j++){
                for(int k=j+1;k<length;k++) {
                    possible_pair_in_sentence.add(new Pair<>(mention_in_sentence.get(j),mention_in_sentence.get(k)));
                }
            }
            output.add(possible_pair_in_sentence);
        }

        return output;
    }


    public List<List<EntityMention>> splitMentionBySentence(List<EntityMention> list){

        int sentenceNum= sentenceIndex.size()-1;
        List<List<EntityMention>> output=new ArrayList<>();

        for(int i=0;i<sentenceNum;i++){
            output.add(new ArrayList<EntityMention>());
        }

        for(EntityMention e: list){
            output.get(e.getSentenceOffset()).add(e);
        }

        for(List<EntityMention> l: output){

            //sort the entity in each sentence by start offset
            Collections.sort(l, new Comparator<EntityMention>() {
                @Override
                public int compare(EntityMention o1, EntityMention o2) {
                    return o1.getExtentStartOffset()-o2.getExtentStartOffset();
                }
            });

        }

        return output;
    }

    /**
     * This method returns all of the relations that are explicitly specified within the gold data
     * @return The list of relations
     */
    public List<Relation> getGoldRelations() {
        return goldRelations;
    }

    public Map<Pair<EntityMention,EntityMention>,Relation> getGoldRelationsByArgs() {
        return goldRelationsByArgs;
    }

    /**
     * This method returns a relation for each pair of entity mentions, including NO_REL relation
     *
     * Note: Coreference "relations" are not included in this!
     * @return The pair of lists of relations; the first list contains the explicit relations, and the second
     *         list contains the "no relation" relations.
     */

    public Pair<List<Relation>,List<Relation>> getAllPairsGoldRelations() {
        List<Relation> result1 = new ArrayList<>(goldRelations);
        List<Relation> result2 = new ArrayList<Relation>(goldRelations);
        for (int e1Ind = 0; e1Ind < goldEntityMentions.size(); e1Ind++) {
            for (int e2Ind = e1Ind + 1; e2Ind < goldEntityMentions.size(); e2Ind++) {
                EntityMention e1 = goldEntityMentions.get(e1Ind);
                EntityMention e2 = goldEntityMentions.get(e2Ind);
                if (!goldRelationsByArgs.containsKey(new Pair<>(e1, e2)) &&
                        !goldRelationsByArgs.containsKey(new Pair<>(e2, e1))) {
                    result2.add(new Relation(Consts.NO_REL, e1, e2));
                }
            }
        }
        return new Pair<>(result1, result2);
    }

    /**
     * This method returns a coreference edge for each pair of entity mentions
     * @return A Pair of lists of edges; the first list contains the "true" edges, and the second
     *         list contains the "false" edges.
     */
    public Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> getAllPairsGoldCoreferenceEdges() {
        List<CoreferenceEdge> posEdges = new ArrayList<>();
        List<CoreferenceEdge> negEdges = new ArrayList<>();
        for (int e1Ind = goldEntityMentions.size()-1; e1Ind >= 0; e1Ind--) {
            boolean foundCoref = false;
            for (int e2Ind = e1Ind - 1; e2Ind >= 0; e2Ind--) {
                EntityMention e1 = goldEntityMentions.get(e1Ind);
                EntityMention e2 = goldEntityMentions.get(e2Ind);
                if (goldCoreferenceEdgesByEntities.containsKey(new Pair<>(e1,e2)) ||
                        goldCoreferenceEdgesByEntities.containsKey(new Pair<>(e2,e1))) {
                    if (!foundCoref) {
                        foundCoref = true;
                        if (goldCoreferenceEdgesByEntities.containsKey(new Pair<>(e1,e2))) {
                            posEdges.add(goldCoreferenceEdgesByEntities.get(new Pair<>(e1,e2)));
                        } else {
                            posEdges.add(goldCoreferenceEdgesByEntities.get(new Pair<>(e2,e1)));
                        }
                    }
                } else if (!(e2.getMentionType().equals(Consts.PRONOUN) && !e1.getMentionType().equals(Consts.PRONOUN))){
                    negEdges.add(new CoreferenceEdge(e2, e1, false));
                }
            }
        }
        return new Pair<>(posEdges, negEdges);
    }

    public List<CoreferenceEdge> getAllPairsTestCoreferenceEdges() {
        Collections.sort(goldEntityMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention e1, EntityMention e2) {
                if (e1.getExtentStartOffset() < e2.getExtentStartOffset()) {
                    return -1;
                } else if (e2.getExtentStartOffset() < e1.getExtentStartOffset()) {
                    return 1;
                } else if (e1.getExtentEndOffset() < e2.getExtentEndOffset()) {
                    return -1;
                } else if (e2.getExtentEndOffset() < e1.getExtentEndOffset()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        List<CoreferenceEdge> result = new ArrayList<>();
        for (int e1Ind = goldEntityMentions.size()-1; e1Ind >= 0; e1Ind--) {
            for (int e2Ind = e1Ind - 1; e2Ind >= 0; e2Ind--) {
                EntityMention e1 = goldEntityMentions.get(e1Ind);
                EntityMention e2 = goldEntityMentions.get(e2Ind);
                boolean isCoreferent = false;
                if (goldCoreferenceEdgesByEntities.containsKey(new Pair<>(e1,e2)) ||
                        goldCoreferenceEdgesByEntities.containsKey(new Pair<>(e2,e1))) {
                    isCoreferent = true;
                }
                if (!(e2.getMentionType().equals(Consts.PRONOUN) && !e1.getMentionType().equals(Consts.PRONOUN))){
                    result.add(new CoreferenceEdge(e2, e1, isCoreferent));
                }
            }
        }
        return result;
    }

    public List<CoreferenceEdge> getAllPairsPipelineCoreferenceEdges() {
        Collections.sort(testEntityMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention e1, EntityMention e2) {
                if (e1.getExtentStartOffset() < e2.getExtentStartOffset()) {
                    return -1;
                } else if (e2.getExtentStartOffset() < e1.getExtentStartOffset()) {
                    return 1;
                } else if (e1.getExtentEndOffset() < e2.getExtentEndOffset()) {
                    return -1;
                } else if (e2.getExtentEndOffset() < e1.getExtentEndOffset()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        List<CoreferenceEdge> result = new ArrayList<>();
        for (int e1Ind = testEntityMentions.size()-1; e1Ind >= 0; e1Ind--) {
            for (int e2Ind = e1Ind - 1; e2Ind >= 0; e2Ind--) {
                EntityMention e1 = testEntityMentions.get(e1Ind);
                EntityMention e2 = testEntityMentions.get(e2Ind);
                if (!(e2.getMentionType().equals(Consts.PRONOUN) && !e1.getMentionType().equals(Consts.PRONOUN))){
                    result.add(new CoreferenceEdge(e2, e1));
                }
            }
        }
        return result;
    }

    public List<CoreferenceEdge> getGoldCoreferenceEdges() {
        return goldCoreferenceEdges;
    }

    public Map<Pair<EntityMention,EntityMention>, CoreferenceEdge> getGoldCoreferenceEdgesByEntities() {
        return goldCoreferenceEdgesByEntities;
    }

    public List<CoreferenceEdge> getTestCoreferenceEdges() {
        return testCoreferenceEdges;
    }

    public List<String> getExtent(int start, int end) {
        return Arrays.asList(ta.getTokensInSpan(start, end));
    }

    public IntPair getNERPrecisionInfo() {
        //precision: out of the predicted mentions, which ones are correct?
        int correct = 0;
        for (EntityMention testEntity: testEntityMentions) {
            IntPair testSpan = new IntPair(testEntity.getHeadStartOffset(), testEntity.getHeadEndOffset());
            if (goldEntityMentionsByHeadSpan.containsKey(testSpan) &&
                    goldEntityMentionsByHeadSpan.get(testSpan).equalsHead(testEntity)) {
                correct++;
            }
        }
        return new IntPair(correct, testEntityMentions.size());
    }

    public IntPair getNERRecallInfo() {
        //Next, recall: out of the correct mentions, how many were predicted?
        int correct = 0;
        for (EntityMention goldEntity: goldEntityMentions) {
            IntPair goldSpan = new IntPair(goldEntity.getHeadStartOffset(), goldEntity.getHeadEndOffset());
            if (testEntityMentionsBySpan.containsKey(goldSpan) &&
                    testEntityMentionsBySpan.get(goldSpan).equalsHead(goldEntity)) {
                correct++;
            }
        }
        return new IntPair(correct, goldEntityMentions.size());

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
        return new IntPair(ta.getTokenIdFromCharacterOffset(mentionStart), ta.getTokenIdFromCharacterOffset(mentionEnd)+1);
    }

    public static void printSentence(List<String> sentence){

        for(String token: sentence){
            System.out.print(token+" ");
        }
        System.out.println();
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

    public static Set<String> getMentionTypes() {
        return mentionTypes;
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






    //reading & writing in local machine

    public void writeToFile(String file) throws IOException {
        File fout = new File(file);
        fout.getParentFile().mkdirs();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fout));
        oos.writeObject(this);
        oos.close();
    }

    public static ACEAnnotation readFromFile(String file) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(file)));
        ACEAnnotation result = null;
        try {
            result = (ACEAnnotation) ois.readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

    public static ACEAnnotation readFileByID( int id ) throws IOException{
        return ACEAnnotation.readFromFile("documents/file_" + id);
    }

    //read and write on all 325 files(5 folders, 65 files each)

    public static void writeAlltoFile(List<List<ACEAnnotation>> splits) throws IOException{

        int count=0;
        for(List<ACEAnnotation> i: splits ){
            for(ACEAnnotation j: i) {
                j.writeToFile("documents/file_" + count);
                System.out.println("#"+count+" save successfully");
                count++;
            }
        }

    }

    public static List<List<ACEAnnotation>> readAllFromFile() throws IOException{

        List<List<ACEAnnotation>> data = new ArrayList<>();
        int count=0;
        for(int i=0;i<5;i++){
            List<ACEAnnotation> annotations = new ArrayList<>();
            for(int j=0;j<65;j++){
                annotations.add(ACEAnnotation.readFromFile("documents/file_" + count));
                System.out.println("#"+count+" load successfully");
                count++;
            }

            data.add(annotations);
        }

        return data;
    }

    public static List<ACEAnnotation> readAllFromFileFlat() throws IOException{

        List<ACEAnnotation> data = new ArrayList<>();
        int count=0;
        for(int i=0;i<5;i++){
            for(int j=0;j<65;j++){
                data.add(ACEAnnotation.readFromFile("documents/file_" + count));
                System.out.println("#"+count+" load successfully");
                count++;
            }
        }
        return data;
    }

}
