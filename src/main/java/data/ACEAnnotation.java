package data;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.View;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.*;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.reader.util.EventConstants;
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
    }


    private String id;
    private List<TextAnnotation> taList;

    // Each sentence is represented as a List of tokens - this is a list of those lists
    private List<List<String>> sentenceTokens = new ArrayList<>();

    //This list simply contains all of the tokens in a flat list - useful when you don't care about sentence boundaries
    private List<String> tokens = new ArrayList<>();
    private List<EntityMention> goldEntityMentions = new ArrayList<>();
    private Map<String,EntityMention> goldEntityMentionsByID = new HashMap<>();
    private List<EntityMention> testEntityMentions = new ArrayList<>();
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

        taList = AceFileProcessor.populateTextAnnotation(doc);

        // Since there may be multiple text annotations, each holding multiple sentences, we make accessing sentences
        // easier
        int count=0;
        for (TextAnnotation ta: taList) {
            Pipeline.addAllViews(ta);
            for (Sentence sentence: ta.sentences()) {
                List<String> sentenceArray=Arrays.asList(sentence.getTokens());
                sentenceTokens.add(sentenceArray);
                tokens.addAll(sentenceArray);
                sentenceIndex.add(count);
                count+=sentenceArray.size();
            }
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
                if (e1.getStartOffset() < e2.getStartOffset()) {
                    return -1;
                } else if (e2.getStartOffset() < e1.getStartOffset()) {
                    return 1;
                } else if (e1.getEndOffset() < e2.getEndOffset()) {
                    return -1;
                } else if (e2.getEndOffset() < e1.getEndOffset()) {
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
        IntPair offsets = findTokenOffsets(mention.extentStart, mention.extentEnd);
        int sentenceOffset=FindSentenceIndex(offsets.getFirst());
        return new EntityMention(type, mention.type, offsets.getFirst(), offsets.getSecond(), sentenceOffset, this);
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
        for (TextAnnotation ta: taList) {
            if (!ta.hasView("NER_ACE_COARSE")) {
                for (Sentence sentence: ta.sentences()) {
                    List<String> labelList = new ArrayList<>();
                    BIOencoding.add(labelList);
                    for (String token: sentence.getTokens()) {
                        labelList.add(Consts.BIO_O);
                    }
                }
            }
            else {
                View nerView = ta.getView("NER_ACE_COARSE");
                int tokenInd = 0;

                List<Constituent> labels = nerView.getConstituents();
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
        return BIOencoding;
    }

    /**
     *
     * @return A list of lists - each of these representing a sentence - of POS tags (representing the tag per
     *         word in the given sentence)
     */
    public List<List<String>> getPOSTagsBySentence() {
        List<List<String>> result = new ArrayList<List<String>>();
        for (TextAnnotation ta: taList) {
            int tokenInd = 0;
            for (Sentence sentence: ta.sentences()) {
                List<String> posList = new ArrayList<>();
                result.add(posList);
                View posView = ta.getView(ViewNames.POS);
                for (int i = 0; i < sentence.getTokens().length; i++) {
                    posList.add(posView.getLabelsCoveringToken(tokenInd++).get(0));
                }
            }
        }

        return result;
    }

    public List<String> getPOSTags() {
        List<String> result = new ArrayList<>();
        for (TextAnnotation ta: taList) {
            int tokenInd = 0;
            for (Sentence sentence: ta.sentences()) {
                View posView = ta.getView(ViewNames.POS);
                for (int i = 0; i < sentence.getTokens().length; i++) {
                    result.add(posView.getLabelsCoveringToken(tokenInd++).get(0));
                }
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
        for (TextAnnotation ta: taList) {
            int tokenInd = 0;
            for (Sentence sentence: ta.sentences()) {
                List<String> lemmaList = new ArrayList<>();
                result.add(lemmaList);
                View lemmaView = ta.getView(ViewNames.LEMMA);
                for (int i = 0; i < sentence.getTokens().length; i++) {
                    lemmaList.add(lemmaView.getLabelsCoveringToken(tokenInd++).get(0));
                }
            }
        }

        return result;
    }

    public List<String> getLemmas() {
        List<String> result = new ArrayList<>();
        for (TextAnnotation ta: taList) {
            int tokenInd = 0;
            for (Sentence sentence: ta.sentences()) {
                View posView = ta.getView(ViewNames.LEMMA);
                for (int i = 0; i < sentence.getTokens().length; i++) {
                    result.add(posView.getLabelsCoveringToken(tokenInd++).get(0));
                }
            }
        }
        return result;
    }

    public void getHeadNounPhrase() {
        for (TextAnnotation ta: taList) {
            System.out.println(ta.getView(ViewNames.PARSE_STANFORD));

        }
    }

    /**
     * This is the function that should be called by the NER system to add an entity mention to the test labels
     *
     * @param type The type of the entity
     * @param startOffset The index of the first token of the span containing the entity
     * @param endOffset The index of the last token + 1 (e.g. if the last token is #3, the value here should be 4)
     */
    public void addEntityMention(String type, int startOffset, int endOffset) {
        testEntityMentions.add(new EntityMention(type, null, startOffset, endOffset, FindSentenceIndex(startOffset), this));
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

            //sort the entity in each sentence by start offset
            Collections.sort(mention_in_sentence, new Comparator<EntityMention>() {
                @Override
                public int compare(EntityMention o1, EntityMention o2) {
                    return o1.getStartOffset()-o2.getStartOffset();
                }
            });

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

    public ArrayList<List<EntityMention>> splitMentionBySentence(List<EntityMention> list){

        int sentenceNum= sentenceIndex.size()-1;
        ArrayList<List<EntityMention>> output=new ArrayList<>();
        for(int i=0;i<sentenceNum;i++){
            output.add(new ArrayList<EntityMention>());
        }

        for(EntityMention e: list){
            output.get(e.getSentenceOffset()).add(e);
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
                    negEdges.add(new CoreferenceEdge(e2, e1));
                }
            }
        }
        return new Pair<>(posEdges, negEdges);
    }

    public List<CoreferenceEdge> getAllPairsTestCoreferenceEdges() {
        Collections.sort(testEntityMentions, new Comparator<EntityMention>() {
            @Override
            public int compare(EntityMention e1, EntityMention e2) {
                if (e1.getStartOffset() < e2.getStartOffset()) {
                    return -1;
                } else if (e2.getStartOffset() < e1.getStartOffset()) {
                    return 1;
                } else if (e1.getEndOffset() < e2.getEndOffset()) {
                    return -1;
                } else if (e2.getEndOffset() < e1.getEndOffset()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        List<CoreferenceEdge> result = new ArrayList<>();
        for (int e1Ind = testEntityMentions.size()-1; e1Ind >= 0; e1Ind--) {
            for (int e2Ind = e1Ind - 1; e2Ind >= 0; e2Ind--) {
                EntityMention e1 = goldEntityMentions.get(e1Ind);
                EntityMention e2 = goldEntityMentions.get(e2Ind);
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
