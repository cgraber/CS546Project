package experiments;

import data.ACEAnnotation;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.edison.annotators.GazetteerViewGenerator;
import edu.illinois.cs.cogcomp.edison.features.*;
import edu.illinois.cs.cogcomp.edison.features.factory.*;
import edu.illinois.cs.cogcomp.edison.utilities.EdisonException;
import learn.PipelineStage;
import utils.Consts;

import javax.swing.text.html.parser.Entity;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Colin Graber on 3/29/16.
 */
public class NERBaseline implements PipelineStage {

    private static final String HEAD_FEATURE_FILE = "ner_mallet_head_features";
    private static final String EXTENT_FEATURE_FILE = "ner_mallet_extent_features";
    private static final String NER_HEAD_MODEL_FILE = "ner_model_head";
    private static final String NER_EXTENT_MODEL_FILE = "ner_model_extent";



    public void trainModel(List<ACEAnnotation> data) {
        buildHeadFeatureFile(data, true);
        buildExtentFeatureFile(data, true);
        runMalletTrain();
    }

    public void test(List<ACEAnnotation> data) {
        buildHeadFeatureFile(data, false);
        List<List<String>> testTags = runMalletTest(true);
        System.out.println(testTags);
        int tagSentInd = 0;
        for (ACEAnnotation doc: data) {
            int tokenInd = 0;

            //System.out.println("NEW DOC: "+doc.getNumberOfSentences());
            int currentStart = 0;
            int currentEnd = 1;
            String currentType = null;
            for (int sentInd = 0; sentInd < doc.getNumberOfSentences(); sentInd++) {
                for (int tagLabelInd = 0; tagLabelInd < doc.getSentence(sentInd).size(); tagLabelInd++) {
                    String tag = testTags.get(tagSentInd).get(tagLabelInd);
                    if (tag.startsWith(Consts.BIO_B)) {
                        if (currentType != null) {
                            //IntPair extent = doc.findMentionExtent(currentStart, currentEnd);
                            doc.addEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd);
                            //System.out.println("Adding entity of type " + currentType + ", (" + currentStart + ", " + currentEnd + ")");

                            currentStart = currentEnd;
                            currentEnd++;
                        } else {
                            currentEnd = currentStart+1;
                        }
                        currentType = tag.split("_")[1];
                        if (tagLabelInd == doc.getSentence(sentInd).size() - 1) {
                            //Case when unit-sized mention at end of sentence
                            doc.addEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd);
                        }
                    } else if (tag.startsWith(Consts.BIO_I)) {
                        currentEnd++;
                        if (tagLabelInd == doc.getSentence(sentInd).size() - 1) {
                            //Case when mention boundary is end of sentence
                            doc.addEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd);
                        }
                    } else {
                        if (currentType != null) {

                            //IntPair extent = doc.findMentionExtent(currentStart, currentEnd);
                            doc.addEntityMention(currentType, currentEnd, currentEnd, currentStart, currentEnd);
                            //System.out.println("Adding entity of type " + currentType + ", (" + currentStart + ", " + currentEnd + ")");

                            currentType = null;
                            currentStart = currentEnd;
                        }
                        currentStart++;

                    }
                }
                tokenInd += doc.getSentence(sentInd).size();
                tagSentInd++;
            }
        }

        buildExtentFeatureFile(data, false);
        testTags = runMalletTest(false);
        tagSentInd = 0;
        for (ACEAnnotation doc: data) {
            List<List<EntityMention>> headMentions = doc.splitMentionBySentence(doc.getTestEntityMentions());
            doc.clearTestEntityMentions();
            int sentOffset = 0;
            for (int sentInd = 0; sentInd < doc.getNumberOfSentences(); sentInd++) {
                List<EntityMention> headsInSentence = headMentions.get(sentInd);
                List<String> sentence = doc.getSentence(sentInd);

                for (EntityMention e: headsInSentence) {
                    int start = 0;
                    int end = 0;
                    boolean foundMention = false;
                    System.out.println(sentence);
                    System.out.println(testTags.get(tagSentInd));
                    for (int tagLabelInd = 0; tagLabelInd < sentence.size(); tagLabelInd++) {
                        String tag = testTags.get(tagSentInd).get(tagLabelInd);

                        if (tag.startsWith(Consts.BIO_B)) {
                            start = sentOffset + tagLabelInd;
                            foundMention = true;
                            if (tagLabelInd == sentence.size() - 1) {
                                end = sentOffset + tagLabelInd + 1;
                            }
                        } else if (tag.startsWith(Consts.BIO_O) && foundMention) {
                            end = sentOffset + tagLabelInd;
                        } else if (tagLabelInd == sentence.size() - 1 && tag.startsWith(Consts.BIO_I)) {
                            end = sentOffset + tagLabelInd + 1;
                        }
                    }
		if (e.getMentionType() == null){
			System.out.println("ERROR: MENTION TYPE IS NULL!");
		}
                    doc.addEntityMention(e.getEntityType(), start, end, e.getHeadStartOffset(), e.getHeadEndOffset());
                    tagSentInd++;
                }


                sentOffset += sentence.size();
            }

        }

    }

    private void buildHeadFeatureFile(List<ACEAnnotation> data, boolean isTrain) {
        StringBuilder fullFeatures = new StringBuilder();
        for (ACEAnnotation doc: data) {
            fullFeatures.append(extractHeadFeatures(doc, isTrain));
            fullFeatures.append("\n");
        }
        writeFeatureFile(fullFeatures.toString(), HEAD_FEATURE_FILE);
    }

    private void buildExtentFeatureFile(List<ACEAnnotation> data, boolean isTrain) {
        StringBuilder fullFeatures = new StringBuilder();
        for (ACEAnnotation doc: data) {
            List<List<EntityMention>> entitiesPerSentence = null;
            if (isTrain) {
                entitiesPerSentence = doc.splitMentionBySentence(doc.getGoldEntityMentions());
            } else {
                entitiesPerSentence = doc.splitMentionBySentence(doc.getTestEntityMentions());
            }
            fullFeatures.append(extractExtentFeatures(doc, entitiesPerSentence, isTrain));
            fullFeatures.append("\n");
        }

        writeFeatureFile(fullFeatures.toString(), EXTENT_FEATURE_FILE);
    }

    /**
     * Extracts the features and then formats them in the manner expected by mallet
     * @param doc The document whose features are being extracted
     * @return A StringBuilder instance holding the features in the right format
     */
    private StringBuilder extractHeadFeatures(ACEAnnotation doc, boolean isTrain) {
        StringBuilder result = new StringBuilder();
        List<List<String>> bioLabels = doc.getGoldBIOEncoding();
        System.out.println(bioLabels);
        List<List<String>> posTags = doc.getPOSTagsBySentence();
        int sentenceOffset = 0;

        for (int sentInd = 0; sentInd < doc.getNumberOfSentences(); sentInd++) {
            List<String> sentence = doc.getSentence(sentInd);

            for (int tokenInd = 0; tokenInd < sentence.size(); tokenInd++) {
                List<Feature> features = extractHeadFeatures(doc.getTA(), sentenceOffset+tokenInd, tokenInd > 0, tokenInd < (sentence.size()-1));
                result.append(convertFeaturesToMalletFormat(features));
                if (isTrain) {
                    result.append(" ").append(bioLabels.get(sentInd).get(tokenInd));
                }
                result.append("\n");
            }
            sentenceOffset += sentence.size();
            result.append("\n");
        }
        return result;
    }

    private StringBuilder extractExtentFeatures(ACEAnnotation doc, List<List<EntityMention>> mentionsPerSentence, boolean isTrain) {
        StringBuilder result = new StringBuilder();
        List<List<List<String>>> bioLabels = null;
        if (isTrain) {
            bioLabels  = doc.getGoldNERExtentBIOEncoding();
        }
        List<List<String>> posTags = doc.getPOSTagsBySentence();
        int sentenceOffset = 0;
        for (int sentInd = 0; sentInd < doc.getNumberOfSentences(); sentInd++) {
            List<String> sentence = doc.getSentence(sentInd);
            List<List<String>> bioForSentence = null;
            if (isTrain) {
                bioForSentence = bioLabels.get(sentInd);
            }
            List<EntityMention> mentions = mentionsPerSentence.get(sentInd);
            for (int mentionInd = 0; mentionInd < mentions.size(); mentionInd++) {
                List<String> bio = null;
                if (isTrain) {
                     bio = bioForSentence.get(mentionInd);
                }
                if (mentionsPerSentence.get(sentInd).isEmpty()) {
                    continue;
                }
                EntityMention e = mentionsPerSentence.get(sentInd).get(mentionInd);
                for (int tokenInd = 0; tokenInd < sentence.size(); tokenInd++) {
                    List<Feature> features = extractExtentFeatures(doc.getTA(), sentenceOffset + tokenInd, e.getHeadStartOffset(), e.getHeadEndOffset());
                    result.append(convertFeaturesToMalletFormat(features));

                    //TODO: add Pronoun-based feature

                    //TODO: other features!
                    */
                    if (isTrain) {
                        result.append(" ").append(bio.get(tokenInd));
                    }

                    result.append("\n");
                }
                result.append("\n");
            }
            sentenceOffset += sentence.size();
        }
        return result;
    }

    private void writeFeatureFile(String info, String path) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)))) {
            writer.write(info);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void runMalletTrain() {
        Runtime rt = Runtime.getRuntime();
        try {
            String [] toExecute = new String[] {"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                    "--train", "true", "--model-file", NER_HEAD_MODEL_FILE, HEAD_FEATURE_FILE};
            System.out.println(toExecute);
            Process pr = rt.exec(toExecute);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            String line=null;

            while ((line =input.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Exit code: " + pr.waitFor());

            toExecute = new String[] {"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                    "--train", "true", "--model-file", NER_EXTENT_MODEL_FILE, EXTENT_FEATURE_FILE};
            System.out.println(toExecute);
            pr = rt.exec(toExecute);
            input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            line=null;

            while ((line =input.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Exit code: " + pr.waitFor());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private List<List<String>> runMalletTest(boolean isHead) {
        Runtime rt = Runtime.getRuntime();
        try {
            String[] toExecute = null;
            if (isHead) {
                toExecute = new String[]{"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                        "--model-file", NER_HEAD_MODEL_FILE, HEAD_FEATURE_FILE};
            } else {
                toExecute = new String[]{"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                        "--model-file", NER_EXTENT_MODEL_FILE, EXTENT_FEATURE_FILE};
            }
            System.out.println(toExecute);
            Process pr = rt.exec(toExecute);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line=null;

            List<List<String>> tags = new ArrayList<List<String>>();
            List<String> currentTags = null;
            while ((line =input.readLine()) != null) {
                if (line.isEmpty()) {
                    currentTags = null;
                } else {
                    if (currentTags == null) {
                        currentTags = new ArrayList<>();
                        tags.add(currentTags);
                    }
                    currentTags.add(line.trim());
                }
            }
            return tags;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public Pair<Double,Double> evaluateExtent(List<ACEAnnotation> docs) {
        int precisionCount = 0;
        double precisionCorrectCount = 0;
        int recallCount = 0;
        double recallCorrectCount = 0;
        for (ACEAnnotation doc: docs) {
            IntPair counts = doc.getNERExtentPrecisionInfo();
            precisionCorrectCount += counts.getFirst();
            precisionCount += counts.getSecond();
            counts = doc.getNERExtentRecallInfo();
            recallCorrectCount += counts.getFirst();
            recallCount += counts.getSecond();
        }
        return new Pair<>(precisionCorrectCount/precisionCount, recallCorrectCount/recallCount);
    }

    public Pair<Double,Double> evaluateHead(List<ACEAnnotation> docs) {
        int precisionCount = 0;
        double precisionCorrectCount = 0;
        int recallCount = 0;
        double recallCorrectCount = 0;
        for (ACEAnnotation doc: docs) {
            IntPair counts = doc.getNERHeadPrecisionInfo();
            precisionCorrectCount += counts.getFirst();
            precisionCount += counts.getSecond();
            counts = doc.getNERHeadRecallInfo();
            recallCorrectCount += counts.getFirst();
            recallCount += counts.getSecond();
        }
        return new Pair<>(precisionCorrectCount/precisionCount, recallCorrectCount/recallCount);
    }

    private static NgramFeatureExtractor bigrams = NgramFeatureExtractor.bigrams(WordFeatureExtractorFactory.word);
    private static NgramFeatureExtractor trigrams = NgramFeatureExtractor.trigrams(WordFeatureExtractorFactory.word);
    private static ContextFeatureExtractor prev = new ContextFeatureExtractor(1, true, false);
    private static ContextFeatureExtractor prevTwo = new ContextFeatureExtractor(2, true, false);
    private static GazetteerViewGenerator gvg;
    private static WordFeatureExtractor gazetteers;

    static {
        try {
            gvg = GazetteerViewGenerator.gazetteersInstance;
            gazetteers = WordFeatureExtractorFactory.getGazetteerFeatureExtractor(ViewNames.GAZETTEER, gvg);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public List<Feature> extractHeadFeatures(TextAnnotation ta, int index, boolean usePrev, boolean useNext) {
        List<Constituent> constituents = ta.getView(ViewNames.TOKENS).getConstituentsCoveringToken(index);
        assert(constituents.size() == 1);
        Constituent constituent = constituents.get(0);
        Set<Feature> features = new HashSet<>();
        try {
            features.addAll(WordFeatureExtractorFactory.word.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index));
            features.addAll(bigrams.getWordFeatures(ta, index));
            features.addAll(trigrams.getWordFeatures(ta, index));
            features.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(constituent));
            features.addAll(ListFeatureFactory.months.getFeatures(constituent));
            features.addAll(ListFeatureFactory.possessivePronouns.getFeatures(constituent));
            features.addAll(prev.getFeatures(constituent));
            features.addAll(prevTwo.getFeatures(constituent));
            features.addAll(gazetteers.getWordFeatures(ta, index));
            if (usePrev) {
                String prefix = "__PREV__";
                List<Feature> previous = new ArrayList<>();
                Constituent prevConst = ta.getView(ViewNames.TOKENS).getConstituentsCoveringToken(index-1).get(0);
                previous.addAll(WordFeatureExtractorFactory.word.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index-1));
                previous.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(prevConst));
                previous.addAll(ListFeatureFactory.months.getFeatures(prevConst));
                previous.addAll(ListFeatureFactory.possessivePronouns.getFeatures(prevConst));
                previous.addAll(gazetteers.getWordFeatures(ta, index-1));
                for (Feature prevFeat: previous) {
                    features.add(prevFeat.prefixWith(prefix));
                }
            }
            if (useNext) {
                String prefix = "__NEXT__";
                List<Feature> next = new ArrayList<>();
                Constituent nextConst = ta.getView(ViewNames.TOKENS).getConstituentsCoveringToken(index + 1).get(0);
                next.addAll(WordFeatureExtractorFactory.word.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index + 1));
                next.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(nextConst));
                next.addAll(ListFeatureFactory.months.getFeatures(nextConst));
                next.addAll(ListFeatureFactory.possessivePronouns.getFeatures(nextConst));
                next.addAll(gazetteers.getWordFeatures(ta, index + 1));
                for (Feature nextFeat : next) {
                    features.add(nextFeat.prefixWith(prefix));
                }
            }
        } catch (EdisonException e) {
            e.printStackTrace();
            System.exit(1);
        }
        List<Feature> result = new ArrayList<>();
        result.addAll(FeatureUtilities.conjoin(features,features));
        return result;
    }

    public List<Feature> extractExtentFeatures(TextAnnotation ta, int index, int headStartOffset, int headEndOffset) {
        List<Constituent> constituents = ta.getView(ViewNames.TOKENS).getConstituentsCoveringToken(index);
        assert(constituents.size() == 1);
        Constituent constituent = constituents.get(0);
        Set<Feature> features = new HashSet<>();
        try {
            features.addAll(WordFeatureExtractorFactory.word.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index));
            features.addAll(bigrams.getWordFeatures(ta, index));
            features.addAll(trigrams.getWordFeatures(ta, index));
            features.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(constituent));
            features.addAll(ListFeatureFactory.months.getFeatures(constituent));
            features.addAll(ListFeatureFactory.possessivePronouns.getFeatures(constituent));
            features.addAll(prev.getFeatures(constituent));
            features.addAll(prevTwo.getFeatures(constituent));
            features.addAll(gazetteers.getWordFeatures(ta, index));

            Constituent headConst = new Constituent(null, null, ta, headStartOffset, headEndOffset);
            Constituent fullConst = null;
            Constituent wordConst = new Constituent(null, null, ta, index, index+1);
            Relation rel = new Relation(null, headConst, wordConst, 0.0f);
            features.addAll(ParsePath.STANFORD.getFeatures(wordConst));
            features.addAll(ParseSiblings.STANFORD.getFeatures(wordConst));
            Set<Feature> parseFeats = ParsePhraseType.STANFORD.getFeatures(wordConst);
            features.addAll(parseFeats);
            String prefix;
            if (index < headStartOffset) {
                prefix = "__BEFORE_HEAD__";
                fullConst = new Constituent(null, null, ta, index, headEndOffset);
            } else if (index >= headEndOffset) {
                prefix = "__IN_HEAD__";
                fullConst = new Constituent(null, null, ta, headStartOffset, index+1);
            } else {
                prefix = "__AFTER_HEAD__";
            }
            if (fullConst != null) {
                features.addAll(FeatureUtilities.prefix("__WITH_HEAD__", ParseHeadWordPOS.STANFORD.getFeatures(fullConst)));
                Set<Feature> parseFeatsWithHead = FeatureUtilities.prefix("__WITH_HEAD__", ParsePhraseType.STANFORD.getFeatures(fullConst)));
                features.addAll(parseFeatsWithHead);
                features.addAll(FeatureUtilities.conjoin(parseFeats, parseFeatsWithHead));
            }


            features.addAll(FeatureUtilities.prefix(prefix, features));
        } catch (EdisonException e) {
            e.printStackTrace();
            System.exit(1);
        }

        List<Feature> result = new ArrayList<>();
        result.addAll(FeatureUtilities.conjoin(features,features));
        return result;
    }

    public String convertFeaturesToMalletFormat(List<Feature> features) {
        StringBuilder result = new StringBuilder();
        for (Feature f: features) {
            if (result.length() != 0) {
                result.append(" ");
            }
            result.append(f.getName());
        }
        return result.toString();
    }
}
