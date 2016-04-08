package experiments;

import data.ACEAnnotation;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import learn.PipelineStage;
import utils.Consts;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin Graber on 3/29/16.
 */
public class NERBaseline implements PipelineStage {

    private static final String FEATURE_FILE = "ner_mallet_features";
    private static final String NER_MODEL_FILE = "ner_model";

    public void trainModel(List<ACEAnnotation> data) {
        buildFeatureFile(data, true);
        runMalletTrain();
    }

    public void test(List<ACEAnnotation> data) {
        buildFeatureFile(data, false);
        List<List<String>> testTags = runMalletTest();
        System.out.println(testTags);
        int tagSentInd = 0;
        for (ACEAnnotation doc: data) {
            System.out.println("NEW DOC: "+doc.getNumberOfSentences());
            int currentStart = 0;
            int currentEnd = 1;
            String currentType = null;
            for (int sentInd = 0; sentInd < doc.getNumberOfSentences(); sentInd++) {
                for (int tagLabelInd = 0; tagLabelInd < doc.getSentence(sentInd).size(); tagLabelInd++) {
                    String tag = testTags.get(tagSentInd).get(tagLabelInd);
                    if (tag.startsWith(Consts.BIO_B)) {
                        if (currentType != null) {
                            IntPair extent = doc.findMentionExtent(currentStart, currentEnd);
                            doc.addEntityMention(currentType, extent.getFirst(), extent.getSecond(), currentStart, currentEnd);
                            //System.out.println("Adding entity of type " + currentType + ", (" + currentStart + ", " + currentEnd + ")");

                            currentStart = currentEnd;
                            currentEnd++;
                        } else {
                            currentEnd = currentStart+1;
                        }
                        currentType = tag.split("_")[1];
                    } else if (tag.startsWith(Consts.BIO_I)) {
                        currentEnd++;
                    } else {
                        if (currentType != null) {
                            IntPair extent = doc.findMentionExtent(currentStart, currentEnd);
                            doc.addEntityMention(currentType, extent.getFirst(), extent.getSecond(), currentStart, currentEnd);
                            //System.out.println("Adding entity of type " + currentType + ", (" + currentStart + ", " + currentEnd + ")");

                            currentType = null;
                            currentStart = currentEnd;
                        }
                        currentStart++;

                    }

                }

                tagSentInd++;
            }
        }
    }

    private void buildFeatureFile(List<ACEAnnotation> data, boolean isTrain) {
        StringBuilder fullFeatures = new StringBuilder();
        for (ACEAnnotation doc: data) {
            fullFeatures.append(extractFeatures(doc, isTrain));
            fullFeatures.append("\n");
        }
        writeFeatureFile(fullFeatures.toString());
    }

    /**
     * Extracts the features and then formats them in the manner expected by mallet
     * @param doc The document whose features are being extracted
     * @return A StringBuilder instance holding the features in the right format
     */
    private StringBuilder extractFeatures(ACEAnnotation doc, boolean isTrain) {
        StringBuilder result = new StringBuilder();
        List<List<String>> bioLabels = doc.getGoldBIOEncoding();
        System.out.println(bioLabels);
        List<List<String>> posTags = doc.getPOSTagsBySentence();

        for (int sentInd = 0; sentInd < doc.getNumberOfSentences(); sentInd++) {
            List<String> sentence = doc.getSentence(sentInd);
            for (int tokenInd = 0; tokenInd < sentence.size(); tokenInd++) {
                String token = sentence.get(tokenInd);
                result.append(Consts.WORD_FEATURE+token);
                result.append(" ").append(Consts.POS_FEATURE+posTags.get(sentInd).get(tokenInd));
                result.append(" ").append(Consts.POS_WORD_FEATURE+token+"_"+posTags.get(sentInd).get(tokenInd));
                String prevToken = "NULL";
                String prevPrevToken = "NULL";
                if (tokenInd > 0) {
                    prevToken = sentence.get(tokenInd-1);
                }
                if (tokenInd > 1) {
                    prevPrevToken = sentence.get(tokenInd-2);
                }
                result.append(" ").append(Consts.PREV_FEATURE+prevToken);
                result.append(" ").append(Consts.WORD_PREV_FEATURE+token+"_"+prevToken);
                result.append(" ").append(Consts.PREV_2_WORD+token+"_"+prevToken+"_"+prevPrevToken);
                if (Character.isUpperCase(token.charAt(0))) {
                    result.append(" ").append(Consts.CAPITALIZED);
                }
                if (token.toUpperCase().equals(token)) {
                    result.append(" ").append(Consts.ALL_CAPS);
                }

                //TODO: add Pronoun-based feature

                //TODO: other features!

                if (isTrain) {
                    result.append(" ").append(bioLabels.get(sentInd).get(tokenInd));
                }
                result.append("\n");
            }
            result.append("\n");
        }
        return result;
    }

    private void writeFeatureFile(String info) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FEATURE_FILE)))) {
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
                    "--train", "true", "--model-file", NER_MODEL_FILE, FEATURE_FILE};
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

    private List<List<String>> runMalletTest() {
        Runtime rt = Runtime.getRuntime();
        try {
            String [] toExecute = new String[] {"java", "-cp", ".:lib/mallet.jar:lib/mallet-deps.jar", "cc.mallet.fst.SimpleTagger",
                     "--model-file", NER_MODEL_FILE, FEATURE_FILE};
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

    public Pair<Double,Double> evaluate(List<ACEAnnotation> docs) {
        int precisionCount = 0;
        double precisionCorrectCount = 0;
        int recallCount = 0;
        double recallCorrectCount = 0;
        for (ACEAnnotation doc: docs) {
            IntPair counts = doc.getNERPrecisionInfo();
            precisionCorrectCount += counts.getFirst();
            precisionCount += counts.getSecond();
            counts = doc.getNERRecallInfo();
            recallCorrectCount += counts.getFirst();
            recallCount += counts.getSecond();
        }
        return new Pair<>(precisionCorrectCount/precisionCount, recallCorrectCount/recallCount);
    }
}
