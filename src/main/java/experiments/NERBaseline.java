package experiments;

import data.ACEAnnotation;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.edison.annotators.GazetteerViewGenerator;
import edu.illinois.cs.cogcomp.edison.features.*;
import edu.illinois.cs.cogcomp.edison.features.factory.*;
import edu.illinois.cs.cogcomp.edison.utilities.EdisonException;
import learn.PipelineStage;
import utils.Consts;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Created by Colin Graber on 3/29/16.
 */
public class NERBaseline implements PipelineStage {

    private static final String HEAD_FEATURE_FILE = "ner_mallet_head_features";
    private static final String EXTENT_FEATURE_FILE = "ner_mallet_extent_features";
    private static final String EXTENT_FEATURE_FILE_VECTORS = "ner_mallet_extent_feature_vectors";
    private static final String NER_COARSE_HEAD_MODEL_FILE = "ner_coarse_model_head";
    private static final String NER_COARSE_EXTENT_MODEL_FILE = "ner_coarse_model_extent";
    private static final String NER_FINE_HEAD_MODEL_FILE = "ner_fine_model_head";
    private static final String NER_FINE_EXTENT_MODEL_FILE = "ner_fine_model_extent";


    private static final String POS = "POS";
    private static final String NEG = "NEG";

    private boolean isCoarse;

    public NERBaseline(boolean isCoarse) {
        super();
        this.isCoarse = isCoarse;
    }

    public void trainModel(List<ACEAnnotation> data) {
        //buildHeadFeatureFile(data, true);
        //runMalletTrainHead();
        trainExtentClassifier(data);
    }

    public void test(List<ACEAnnotation> data) {
        buildHeadFeatureFile(data, false);
        List<List<String>> testTags = runMalletTestHead();
        int tagSentInd = 0;
        System.out.println("Finding test head labels...");
        int ind = 0;
        for (ACEAnnotation doc: data) {
            System.out.println("\tFinding head labels for doc " + (ind++));
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
                            if (isCoarse) {
                                doc.addCoarseHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            } else {
                                doc.addFineHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            }
                            //System.out.println("Adding entity of type " + currentType + ", (" + currentStart + ", " + currentEnd + ")");

                            currentStart = currentEnd;
                            currentEnd++;
                        } else {
                            currentEnd = currentStart+1;
                        }
                        currentType = tag.split("_")[1];
                        if (tagLabelInd == doc.getSentence(sentInd).size() - 1) {
                            //Case when unit-sized mention at end of sentence
                            if (isCoarse) {
                                doc.addCoarseHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            } else {
                                doc.addFineHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            }
                        }
                    } else if (tag.startsWith(Consts.BIO_I)) {
                        currentEnd++;
                        if (tagLabelInd == doc.getSentence(sentInd).size() - 1) {
                            //Case when mention boundary is end of sentence
                            if (isCoarse) {
                                doc.addCoarseHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            } else {
                                doc.addFineHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            }
                        }
                    } else {
                        if (currentType != null) {

                            //IntPair extent = doc.findMentionExtent(currentStart, currentEnd);
                            if (isCoarse) {
                                doc.addCoarseHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            } else {
                                doc.addFineHeadEntityMention(currentType, currentStart, currentEnd, currentStart, currentEnd, true);
                            }
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

        tagSentInd = 0;
        System.out.println("Finding test extent information...");
        ind = 0;
        for (ACEAnnotation doc: data) {
            System.out.println("\ttest extent info for doc " + (ind++));
            List<EntityMention> mentions = doc.getTestEntityMentions();
            doc.clearTestEntityMentions();
            int entityInd = 0;
            List<List<Feature>> features = new ArrayList<>();
            for (EntityMention e: mentions) {
                List<String> sentence = doc.getSentence(e.getSentenceOffset());
                int sentenceOffset = doc.getSentenceIndex(e.getSentenceOffset());
                for (int i = sentenceOffset; i < e.getHeadStartOffset(); i++) {
                    features.add(extractExtentFeatures(doc.getTA(), i, e.getHeadStartOffset(), e.getHeadEndOffset(), i > sentenceOffset, i < sentenceOffset+sentence.size()-1));
                }
                for (int i = e.getHeadEndOffset(); i < sentenceOffset + sentence.size(); i++) {
                    features.add(extractExtentFeatures(doc.getTA(), i, e.getHeadStartOffset(), e.getHeadEndOffset(), i > sentenceOffset, i < sentenceOffset+sentence.size()-1));
                }
            }
            System.out.println("\t\tGetting extent labels");
            List<String> labels = getExtentTestLabels(features);
            System.out.println("\t\tFinding extent boundaries");
            int listOffset = 0;
            int count = 0;
            for (EntityMention e: mentions) {
                System.out.println("\t\t\tMention "+ (++count) +" out of "+mentions.size());
                List<String> sentence = doc.getSentence(e.getSentenceOffset());
                int sentenceOffset = doc.getSentenceIndex(e.getSentenceOffset());
                boolean foundStart = false;
                boolean foundEnd = false;
                int extentStart = e.getHeadStartOffset() - 1;
                int extentEnd = e.getHeadEndOffset();
                while (!foundStart && extentStart >= sentenceOffset) {
                    String label = labels.get(extentStart - sentenceOffset + listOffset);
                    if (label.equals(NEG)) {
                        foundStart = true;
                    } else {
                        extentStart--;
                    }
                }
                extentStart++;
                while (!foundEnd && extentEnd < sentenceOffset + sentence.size()) {
                    String label = labels.get((extentEnd - e.getHeadEndOffset()) + (e.getHeadStartOffset() - sentenceOffset) + listOffset);
                    if (label.equals(NEG)) {
                        foundEnd = true;
                    } else {
                        extentEnd++;
                    }   
                }
                if (isCoarse) {
                    doc.addCoarseExtentEntityMention(e.getCoarseEntityType(), extentStart, extentEnd, e.getHeadStartOffset(), e.getHeadEndOffset(), true);
                } else {
                    doc.addFineExtentEntityMention(e.getCoarseEntityType(), extentStart, extentEnd, e.getHeadStartOffset(), e.getHeadEndOffset(), true);
                }
                listOffset += sentence.size() - (e.getHeadEndOffset() - e.getHeadStartOffset());
            }
        }

    }

    private void buildHeadFeatureFile(List<ACEAnnotation> data, boolean isTrain) {
        System.out.println("Extracting head features...");
        StringBuilder fullFeatures = new StringBuilder();
        int ind = 0;
        for (ACEAnnotation doc: data) {
            System.out.println("\textracting head features for doc "+(ind++));
            fullFeatures.append(extractHeadFeatures(doc, isTrain));
            fullFeatures.append("\n");
        }
        writeFeatureFile(fullFeatures.toString(), HEAD_FEATURE_FILE);
    }

    /**
     * Extracts the features and then formats them in the manner expected by mallet
     * @param doc The document whose features are being extracted
     * @return A StringBuilder instance holding the features in the right format
     */
    private StringBuilder extractHeadFeatures(ACEAnnotation doc, boolean isTrain) {
        StringBuilder result = new StringBuilder();
        List<List<String>> bioLabels = null;
        if (isTrain) {
            if (isCoarse) {
                bioLabels = doc.getGoldCoarseBIOEncoding();
            } else {
                bioLabels = doc.getGoldFineBIOEncoding();
            }
        }
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

    private void trainExtentClassifier(List<ACEAnnotation> docs) {
        System.out.println("Training extent classifier...");
        List<Pair<List<Feature>, Boolean>> examples = new ArrayList<>();
        int ind = 0;
        for (ACEAnnotation doc: docs) {
            System.out.println("\tExtracting extent features for doc "+(ind++));
            for (EntityMention e: doc.getGoldEntityMentions()) {
                List<String> sentence = doc.getSentence(e.getSentenceOffset());
                int sentenceOffset = doc.getSentenceIndex(e.getSentenceOffset());
                for (int i = e.getHeadEndOffset(); i < e.getExtentEndOffset(); i++) {
                    examples.add(new Pair(extractExtentFeatures(doc.getTA(), i, e.getHeadStartOffset(), e.getHeadEndOffset(), i > sentenceOffset, i < sentenceOffset+sentence.size()-1), true));
                }
                if (e.getExtentEndOffset() < sentenceOffset + sentence.size()) {
                    examples.add(new Pair(extractExtentFeatures(doc.getTA(), e.getExtentEndOffset(), e.getHeadStartOffset(), e.getHeadEndOffset(), e.getExtentEndOffset() > sentenceOffset, e.getExtentEndOffset() < sentenceOffset+sentence.size()-1), false));
                }
                for (int i = e.getHeadStartOffset() - 1; i >= e.getExtentStartOffset(); i--) {
                    examples.add(new Pair(extractExtentFeatures(doc.getTA(), i, e.getHeadStartOffset(), e.getHeadEndOffset(), i > sentenceOffset, i < sentenceOffset+sentence.size()-1), true));
                }
                if (e.getExtentStartOffset() - 1 > sentenceOffset) {
                    examples.add(new Pair(extractExtentFeatures(doc.getTA(), e.getExtentStartOffset()-1, e.getHeadStartOffset(), e.getHeadEndOffset(), e.getExtentStartOffset() > sentenceOffset, e.getExtentStartOffset() < sentenceOffset+sentence.size()-1), false));
                }
            }
        }
        writeTrainClassifierFile(examples);
        runMalletTrainExtent();
    }

    private void writeTrainClassifierFile(List<Pair<List<Feature>, Boolean>> examples) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(EXTENT_FEATURE_FILE)))) {
            for (int i = 0; i < examples.size(); i++) {
                Pair<List<Feature>, Boolean> example = examples.get(i);
                writer.write(i + " ");
                if (example.getSecond()) {
                    writer.write(POS+ " ");
                } else {
                    writer.write(NEG+ " ");
                }
                writer.write(convertFeaturesToMalletFormat(example.getFirst()));
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    private List<String> getExtentTestLabels(List<List<Feature>> examples) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(EXTENT_FEATURE_FILE)))) {
            for (List<Feature> example: examples) {
                writer.write(convertFeaturesToMalletFormat(example));
                writer.write("\n");
            }
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        Runtime rt = Runtime.getRuntime();
        List<String> results = new ArrayList<String>();
        try {
            String modelFile;
            if (isCoarse) {
                modelFile = NER_COARSE_EXTENT_MODEL_FILE;
            } else {
                modelFile = NER_FINE_EXTENT_MODEL_FILE;
            }
            String[] toExecute = new String[]{"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.classify.tui.Csv2Classify",
                    "--input", EXTENT_FEATURE_FILE, "--output", "-", "--classifier", modelFile};
            Process pr = rt.exec(toExecute);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line = null;
            results = new ArrayList<String>();
            while ((line = input.readLine()) != null) {
                String[] args = line.split("\\s+");
                Map<String, Double> vals = new HashMap<>();
                vals.put(args[1], Double.parseDouble(args[2]));
                vals.put(args[3], Double.parseDouble(args[4]));
                results.add(vals.get(POS) > vals.get(NEG) ? POS : NEG);
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return results;
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

    private void runMalletTrainHead() {
        Runtime rt = Runtime.getRuntime();
        try {
            String modelFile;
            if (isCoarse) {
                modelFile = NER_COARSE_HEAD_MODEL_FILE;
            } else {
                modelFile = NER_FINE_HEAD_MODEL_FILE;
            }
            String [] toExecute = new String[] {"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                    "--train", "true", "--model-file", modelFile, HEAD_FEATURE_FILE};
            System.out.println(toExecute);
            Process pr = rt.exec(toExecute);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            String line=null;

            while ((line =input.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Exit code: " + pr.waitFor());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void runMalletTrainExtent() {
        Runtime rt = Runtime.getRuntime();
        try {

            String [] toExecute = new String[] {"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.classify.tui.Csv2Vectors",
                    "--input", EXTENT_FEATURE_FILE, "--output", EXTENT_FEATURE_FILE_VECTORS};
            System.out.println(toExecute);
            Process pr = rt.exec(toExecute);
            BufferedReader input = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

            String line=null;

            while ((line =input.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Exit code: " + pr.waitFor());
            String modelFile;
            if (isCoarse) {
                modelFile = NER_COARSE_EXTENT_MODEL_FILE;
            } else {
                modelFile = NER_FINE_EXTENT_MODEL_FILE;
            }
            toExecute = new String[] {"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.classify.tui.Vectors2Classify",
                    "--input", EXTENT_FEATURE_FILE_VECTORS, "--output-classifier", modelFile};
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

    private List<List<String>> runMalletTestHead() {
        System.out.println("Running mallet to get test head labels...");
        Runtime rt = Runtime.getRuntime();
        try {
            String[] toExecute = null;

            String modelFile;
            if (isCoarse) {
                modelFile = NER_COARSE_HEAD_MODEL_FILE;
            } else {
                modelFile = NER_FINE_HEAD_MODEL_FILE;
            }
            toExecute = new String[]{"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                        "--model-file", modelFile, HEAD_FEATURE_FILE};


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
        IntPair counts;
        for (ACEAnnotation doc: docs) {
            if (isCoarse) {
                counts = doc.getNERCoarseExtentPrecisionInfo();
                precisionCorrectCount += counts.getFirst();
                precisionCount += counts.getSecond();
                counts = doc.getNERCoarseExtentRecallInfo();
                recallCorrectCount += counts.getFirst();
                recallCount += counts.getSecond();
            } else {
                counts = doc.getNERFineExtentPrecisionInfo();
                precisionCorrectCount += counts.getFirst();
                precisionCount += counts.getSecond();
                counts = doc.getNERFineExtentRecallInfo();
                recallCorrectCount += counts.getFirst();
                recallCount += counts.getSecond();
            }
        }
        return new Pair<>(precisionCorrectCount/precisionCount, recallCorrectCount/recallCount);
    }

    public Pair<Double,Double> evaluateHead(List<ACEAnnotation> docs) {
        int precisionCount = 0;
        double precisionCorrectCount = 0;
        int recallCount = 0;
        double recallCorrectCount = 0;
        IntPair counts;
        for (ACEAnnotation doc: docs) {
            if (isCoarse) {
                counts = doc.getNERCoarseHeadPrecisionInfo();
                precisionCorrectCount += counts.getFirst();
                precisionCount += counts.getSecond();
                counts = doc.getNERCoarseHeadRecallInfo();
                recallCorrectCount += counts.getFirst();
                recallCount += counts.getSecond();
            } else {
                counts = doc.getNERFineHeadPrecisionInfo();
                precisionCorrectCount += counts.getFirst();
                precisionCount += counts.getSecond();
                counts = doc.getNERFineHeadRecallInfo();
                recallCorrectCount += counts.getFirst();
                recallCount += counts.getSecond();
            }
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
                //previous.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index-1));
                //previous.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(prevConst));
                //previous.addAll(ListFeatureFactory.months.getFeatures(prevConst));
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
                //next.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index + 1));
                //next.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(nextConst));
                //next.addAll(ListFeatureFactory.months.getFeatures(nextConst));
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
        //result.addAll(FeatureUtilities.conjoin(features,features));
        result.addAll(features);
        return result;
    }

    public List<Feature> extractExtentFeatures(TextAnnotation ta, int index, int headStartOffset, int headEndOffset, boolean usePrev, boolean useNext) {
        List<Constituent> constituents = ta.getView(ViewNames.TOKENS).getConstituentsCoveringToken(index);
        assert(constituents.size() == 1);
        Constituent constituent = constituents.get(0);
        Set<Feature> features = new HashSet<>();
        try {
            features.addAll(WordFeatureExtractorFactory.word.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index));
            //features.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index));
            //features.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index));
            features.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index));
            //features.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(constituent));
            //features.addAll(ListFeatureFactory.months.getFeatures(constituent));
            features.addAll(ListFeatureFactory.possessivePronouns.getFeatures(constituent));
            features.addAll(gazetteers.getWordFeatures(ta, index));
            if (usePrev) {
                String prefix = "__PREV__";
                List<Feature> previous = new ArrayList<>();
                Constituent prevConst = ta.getView(ViewNames.TOKENS).getConstituentsCoveringToken(index-1).get(0);
                previous.addAll(WordFeatureExtractorFactory.word.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index-1));
                //previous.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index-1));
                previous.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index-1));
                //previous.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(prevConst));
                //previous.addAll(ListFeatureFactory.months.getFeatures(prevConst));
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
                //next.addAll(WordFeatureExtractorFactory.dateMarker.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.wordCase.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.capitalization.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.conflatedPOS.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.lemma.getWordFeatures(ta, index + 1));
                //next.addAll(WordFeatureExtractorFactory.numberNormalizer.getWordFeatures(ta, index + 1));
                next.addAll(WordFeatureExtractorFactory.pos.getWordFeatures(ta, index + 1));
                //next.addAll(ListFeatureFactory.daysOfTheWeek.getFeatures(nextConst));
                //next.addAll(ListFeatureFactory.months.getFeatures(nextConst));
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
        
        Constituent headConst = new Constituent(null, null, ta, headStartOffset, headEndOffset);
        Constituent fullConst = null;
        Constituent wordConst = new Constituent(null, null, ta, index, index+1);
        Relation rel = new Relation(null, headConst, wordConst, 0.0f);
        try {
            features.addAll(ParseSiblings.STANFORD.getFeatures(wordConst));
        }catch (Exception e) {
        }
        try {
            features.addAll(ParsePath.STANFORD.getFeatures(wordConst));
        } catch (Exception e) {
        }

        String prefix;
        if (index < headStartOffset) {
	    prefix = "_BH_";
	    fullConst = new Constituent(null, null, ta, index, headEndOffset);
        } else if (index >= headEndOffset) {
	    prefix = "_IH_";
	    fullConst = new Constituent(null, null, ta, headStartOffset, index+1);
        } else {
	    prefix = "_AH_";
        }
        try {
            Set<Feature> parseFeats = ParsePhraseType.STANFORD.getFeatures(wordConst);
            features.addAll(parseFeats);
            if (fullConst != null) {
                features.addAll(FeatureUtilities.prefix("_WH_", ParseHeadWordPOS.STANFORD.getFeatures(fullConst)));
                Set<Feature> parseFeatsWithHead = FeatureUtilities.prefix("_WH_", ParsePhraseType.STANFORD.getFeatures(fullConst));
                features.addAll(parseFeatsWithHead);
                features.addAll(FeatureUtilities.conjoin(parseFeats, parseFeatsWithHead));
            }
        } catch (Exception e) {
        }

        features = FeatureUtilities.prefix(prefix, features);
        List<Feature> result = new ArrayList<>();
        result.addAll(features);
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
