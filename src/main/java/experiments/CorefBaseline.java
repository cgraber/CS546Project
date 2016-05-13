package experiments;

import java.util.*;

import weka.core.Instances;
import learn.FeatureGenerator;
import learn.PipelineStage;
import data.ACEAnnotation;
import data.CoreferenceEdge;
import data.DataUtils;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import weka.classifiers.functions.Logistic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * The baseline coreference class using simple features and binary classification setup.
 * <p>
 * Created by Alex Morales on 3/21/16.
 */
public class CorefBaseline implements PipelineStage {
    public ArrayList<ACEAnnotation> train;
    public ArrayList<ACEAnnotation> test;
    //private Map<EntityMention, List<CoreferenceEdge>> candidate_map;
    List<Double> prediction_scores;
    //Weka
    public Logistic classifier;
    public static final boolean DEBUG = false;
    public static final String RCSDir = "rcsTestfiles";
    public static final double STHOLD = 0.15;

    public int ap;

    public CorefBaseline() {
        this.train = null;
        this.test = null;
        this.classifier = null;
    }

    public CorefBaseline(List<List<ACEAnnotation>> train) {
        this.train = new ArrayList<ACEAnnotation>();
        for (List<ACEAnnotation> item : train) {
            this.train.addAll(item);
        }
        this.classifier = new Logistic();
    }

    public CorefBaseline(List<List<ACEAnnotation>> train, List<List<ACEAnnotation>> test) {
        this.train = new ArrayList<ACEAnnotation>();
        for (List<ACEAnnotation> item : train) {
            this.train.addAll(item);
        }

        this.test = new ArrayList<ACEAnnotation>();
        for (List<ACEAnnotation> item : train) {
            this.test.addAll(item);
        }
        this.classifier = new Logistic();
    }


    // assuming gold data for train?
    public CorefBaseline(ArrayList<ACEAnnotation> train, ArrayList<ACEAnnotation> test) {
        this.train = new ArrayList<ACEAnnotation>();
        this.train.addAll(train);
        //addCandidates(train);
        this.test = new ArrayList<ACEAnnotation>();
        this.test.addAll(test);

        this.classifier = new Logistic();
    }

    public void addCandidates(ArrayList<ACEAnnotation> data) {
        ;
        for (ACEAnnotation entry : data) {
            Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> temp = entry.getAllPairsGoldCoreferenceEdges();
            List<CoreferenceEdge> positive_examples = temp.getFirst();
        }
    }

    @Override
    public void trainModel(List<ACEAnnotation> data) {
        // TODO Auto-generated method stub
        this.train = new ArrayList<ACEAnnotation>();
        this.train.addAll(data);
        this.classifier = new Logistic();

        this.learn();
    }

    public void trainModelPipeline(List<ACEAnnotation> data) {
        // TODO Auto-generated method stub
        this.train = new ArrayList<ACEAnnotation>();
        this.train.addAll(data);
        this.classifier = new Logistic();

        this.learnPipeline();
    }

    @Override
    public void test(List<ACEAnnotation> data) {
        // add all data to test list
        this.test = new ArrayList<ACEAnnotation>();
        this.test.addAll(data);
        // why not this.test = data?
        //System.out.println("constituents test:" + this.test.get(0).getTestEntityMentions().get(0).getMentionType());
        //System.out.println("constituents data:" + data.get(0).getTestEntityMentions().get(0).getConstituent());
        try {
            this.predict();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testPipeline(List<ACEAnnotation> data) {
        // add all data to test list
        this.test = new ArrayList<ACEAnnotation>();
        this.test.addAll(data);
        // why not this.test = data?
        //System.out.println("constituents test:" + this.test.get(0).getTestEntityMentions().get(0).getMentionType());
        //System.out.println("constituents data:" + data.get(0).getTestEntityMentions().get(0).getConstituent());
        try {
            this.predictPipeline();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Learning with WEKA
    public void learn() {
        Instances trainInstances = FeatureGenerator.readData(this.train, true, true);
        trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        System.out.println("number of training instances:" + trainInstances.numInstances());
        System.out.println("number of attributes in instances:" + (trainInstances.numAttributes() - 1));
        System.out.println("building classifier");
        try {
            classifier.buildClassifier(trainInstances);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.err.println("Unable to build Classifier...\n");
            e.printStackTrace();
        }

        if (CorefBaseline.DEBUG) {
            System.out.println(classifier.getTechnicalInformation());
            System.out.println("learned coefficients:");
            int i = 0;
            System.out.println(classifier.toString());
            System.out.println(classifier.ridgeTipText());
            System.out.println("classifier coefficient lenghts:" + classifier.coefficients().length);
            for (int j = 0; j < classifier.coefficients().length; j++) {
                for (int k = 0; k < classifier.coefficients()[j].length; k++) {
                    System.out.print(i++ + ": " + classifier.coefficients()[j][k] + " (j,k):" + j + "," + k);
                }
                System.out.println();
            }
            try {
                File file = new File("arff/trainData.arff");

                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }

                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(trainInstances.toString());
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // learning using pipeline
    public void learnPipeline() {
        Instances trainInstances = FeatureGenerator.readData(this.train, true, false);
        trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        System.out.println("number of training instances:" + trainInstances.numInstances());
        System.out.println("number of attributes in instances:" + (trainInstances.numAttributes() - 1));
        System.out.println("building classifier");
        try {
            classifier.buildClassifier(trainInstances);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.err.println("Unable to build Classifier...\n");
            e.printStackTrace();
        }

        if (CorefBaseline.DEBUG) {
            System.out.println(classifier.getTechnicalInformation());
            System.out.println("learned coefficients:");
            int i = 0;
            System.out.println(classifier.toString());
            System.out.println(classifier.ridgeTipText());
            System.out.println("classifier coefficient lenghts:" + classifier.coefficients().length);
            for (int j = 0; j < classifier.coefficients().length; j++) {
                for (int k = 0; k < classifier.coefficients()[j].length; k++) {
                    System.out.print(i++ + ": " + classifier.coefficients()[j][k] + " (j,k):" + j + "," + k);
                }
                System.out.println();
            }
            try {
                File file = new File("arff/trainData.arff");

                // if file doesnt exists, then create it
                if (!file.exists()) {
                    file.createNewFile();
                }

                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(trainInstances.toString());
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // here we need to match the entities
    public Map<EntityMention, CoreferenceEdge> stitchPipeline() {
        System.out.println("Stitching Pipeline...");
        int index = 0;
        int jj = 0;
        Map<EntityMention, CoreferenceEdge> candidate_map = null;
        Map<EntityMention, Double> candidate_best = null;

        // first need to find all of the possible mapping? and the best mapping
        for (ACEAnnotation a : this.test) {
            // candidate map should be different for each ace annotation
            candidate_map = new HashMap<EntityMention, CoreferenceEdge>();
            candidate_best = new HashMap<EntityMention, Double>();

            // Following standard that we are not relating a later entity mention to an earlier entityMention??
            List<CoreferenceEdge> pipe_edges = a.getAllPairsPipelineCoreferenceEdges();
            //List<CoreferenceEdge> gold_edges = a.getTestCoreferenceEdges();

            EntityMention em1 = null;
            EntityMention em2 = null;
            Pair<EntityMention, EntityMention> mentions_pair = null;
            for (CoreferenceEdge item : pipe_edges) {
                mentions_pair = item.getEntityMentions();
                if (mentions_pair.getFirst().getHeadStartOffset() < mentions_pair.getSecond().getHeadStartOffset()) {
                    em1 = mentions_pair.getFirst();
                    em2 = mentions_pair.getSecond();
                } else {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("ERROR: shouldn't have em1 > em2");
                        System.out.println("current example first head: " + mentions_pair.getFirst().getHeadStartOffset() + " first extent: "
                                + mentions_pair.getFirst().getExtentStartOffset() + " second: " + mentions_pair.getSecond().getHeadStartOffset()
                                + " second extent: " + mentions_pair.getSecond().getExtentStartOffset());
                    }

                    em2 = mentions_pair.getFirst();
                    em1 = mentions_pair.getSecond();
                }
                // Need to check if there is an analouge entity mention in the gold set?

                // threshold
                if (this.prediction_scores.get(index) < CorefBaseline.STHOLD) {
                    //System.out.println(this.prediction_scores.get(index));
                    index++;
                    continue;
                }

                if (candidate_best.containsKey(em2)) {
                    if (this.prediction_scores.get(index) > candidate_best.get(em2)) {
                        candidate_best.put(em2, this.prediction_scores.get(index));
                        candidate_map.put(em2, item);
                    }
                } else {
                    candidate_best.put(em2, this.prediction_scores.get(index));
                    candidate_map.put(em2, item);
                }
                index++;
            }

            List<CoreferenceEdge> pred_edges = new ArrayList<CoreferenceEdge>();
            // Before sorting
            for (CoreferenceEdge predicted_positive : candidate_map.values()) {
                pred_edges.add(predicted_positive);
            }

            //sort the candidate edges in each sentence by start offset
            Collections.sort(pred_edges, new Comparator<CoreferenceEdge>() {
                @Override
                public int compare(CoreferenceEdge o1, CoreferenceEdge o2) {
                    // first compare entity mention e1, unless they are the same then look at e2.
                    if (o1.e1.getHeadStartOffset() - o2.e1.getHeadStartOffset() == 0) {
                        return o1.e2.getHeadStartOffset() - o2.e2.getHeadStartOffset();
                    }
                    return o1.e1.getHeadStartOffset() - o2.e1.getHeadStartOffset();
                }
            });

            // After sorting
            if (CorefBaseline.DEBUG) {
                System.out.println("Sorted coreference list");
                for (CoreferenceEdge pp : pred_edges) {
                    System.out.println(pp.e1.getHeadStartOffset() + ":" + pp.e1.getConstituent() + " " + pp.e2.getHeadStartOffset() + ":" + pp.e1.getConstituent());
                }
            }

            // inverted index of mentions
            // then need to assign if its coreferent or not.
            // up to here we don't assume that we know the labels
            ArrayList<List<EntityMention>> equivalence_classes = new ArrayList<List<EntityMention>>();
            for (CoreferenceEdge predicted_positive : pred_edges) {
                // constructing equivalence classes
                Boolean found = false;
                mentions_pair = predicted_positive.getEntityMentions();
                if (mentions_pair.getFirst().getHeadStartOffset() < mentions_pair.getSecond().getHeadStartOffset()) {
                    em1 = mentions_pair.getFirst();
                    em2 = mentions_pair.getSecond();
                } else {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("ERROR: shouldn't have em1 > em2");
                        System.out.println("current example first head: " + mentions_pair.getFirst().getHeadStartOffset() + " first extent: "
                                + mentions_pair.getFirst().getExtentStartOffset() + " second: " + mentions_pair.getSecond().getHeadStartOffset()
                                + " second extent: " + mentions_pair.getSecond().getExtentStartOffset());
                    }
                    em2 = mentions_pair.getFirst();
                    em1 = mentions_pair.getSecond();
                }

                int eq_index = 0;
                for (List<EntityMention> ec : equivalence_classes) {
                    if (ec.contains(em1)) {
                        if (!ec.contains(em2)) {
                            ec.add(em2);
                            if (CorefBaseline.DEBUG) {
                                System.out.println("found [em1] ( " + em1.getHeadStartOffset() + ") adding (" + em2.getHeadStartOffset() + "):" + em2.getConstituent() + " \t to ec: " + eq_index);
                            }
                        }
                        found = true;
                        break;
                    } else if (ec.contains(em2)) {
                        if (!ec.contains(em1)) {
                            ec.add(em1);
                            if (CorefBaseline.DEBUG) {
                                System.out.println("found [em2] ( " + em2.getHeadStartOffset() + ") adding (" + em1.getHeadStartOffset() + "):" + em1.getConstituent() + " \t to ec: " + eq_index);
                            }
                        }
                        found = true;
                        break;
                    }
                    eq_index++;
                }

                if (!found) {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("not found (" + em1.getHeadStartOffset() + "):" + em1.getConstituent() + " or (" + em2.getHeadStartOffset() + "):" + em2.getConstituent());
                    }
                    List<EntityMention> ec = new ArrayList<EntityMention>();
                    ec.add(em1);
                    ec.add(em2);
                    equivalence_classes.add(ec);
                }
            }

            if (CorefBaseline.DEBUG) {
                int len = 0;
                for (List<String> sentence : a.getSentences()) {
                    System.out.print(len + ":");
                    for (String word : sentence) {
                        System.out.print(word + " ");
                    }
                    System.out.println();
                    len += sentence.size();
                }
                System.out.println();
            }

            // add the equivalence classes
            int k = 0;
            for (List<EntityMention> ec : equivalence_classes) {
                if (CorefBaseline.DEBUG) {
                    System.out.print("equivilence classes:");
                    System.out.println("ec:" + k++);
                    for (EntityMention em : ec) {
                        System.out.print(em.getHeadStartOffset() + ": ");
                        for (String word : em.getHead()) {
                            System.out.print(word + " ");
                        }
                        // printing candidate map
                        if (candidate_map.containsKey(em)) {
                            System.out.print("\t\t (" + candidate_map.get(em).e1.getHeadStartOffset() + " " + candidate_map.get(em).e2.getHeadStartOffset() + ")");
                            System.out.print("\t\t score: " + this.prediction_scores.get(index - pipe_edges.indexOf(candidate_map.get(em)) - 1));
                        } else {
                            System.out.print("\t\t");
                        }

                        System.out.println("\t\t constituent:" + em.getConstituent());
                    }
                    System.out.println();
                }
                // adding the equivalence classes
                //a.addCoreferentEntity(ec);

            }
            // Collect all ACE annotations?
            CorefBaseline.write2file(a.getSentences(), equivalence_classes, "test-file-" + jj + ".response", jj);
            jj++;
        }
        return candidate_map;
    }

    // new stitch method for testing data
    public Map<EntityMention, CoreferenceEdge> stitch() {
        int index = 0;
        Map<EntityMention, CoreferenceEdge> candidate_map = null;
        Map<EntityMention, Double> candidate_best = null;

        // first need to find all of the possible mapping? and the best mapping
        for (ACEAnnotation a : this.test) {
            // candidate map should be different for each ace annotation
            candidate_map = new HashMap<EntityMention, CoreferenceEdge>();
            candidate_best = new HashMap<EntityMention, Double>();

            // Following standard that we are not relating a later entity mention to an earlier entityMention??
            List<CoreferenceEdge> examples = a.getAllPairsPipelineCoreferenceEdges();
            //List<CoreferenceEdge> examples = a.getTestCoreferenceEdges();

            EntityMention em1 = null;
            EntityMention em2 = null;
            Pair<EntityMention, EntityMention> mentions_pair = null;
            for (CoreferenceEdge item : examples) {
                mentions_pair = item.getEntityMentions();
                if (mentions_pair.getFirst().getHeadStartOffset() < mentions_pair.getSecond().getHeadStartOffset()) {
                    em1 = mentions_pair.getFirst();
                    em2 = mentions_pair.getSecond();
                } else {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("ERROR: shouldn't have em1 > em2");
                        System.out.println("current example first head: " + mentions_pair.getFirst().getHeadStartOffset() + " first extent: "
                                + mentions_pair.getFirst().getExtentStartOffset() + " second: " + mentions_pair.getSecond().getHeadStartOffset()
                                + " second extent: " + mentions_pair.getSecond().getExtentStartOffset());
                    }

                    em2 = mentions_pair.getFirst();
                    em1 = mentions_pair.getSecond();
                }

                // threshold
                if (this.prediction_scores.get(index) < CorefBaseline.STHOLD) {
                    //System.out.println(this.prediction_scores.get(index));
                    index++;
                    continue;
                }

                if (candidate_best.containsKey(em2)) {
                    if (this.prediction_scores.get(index) > candidate_best.get(em2)) {
                        candidate_best.put(em2, this.prediction_scores.get(index));
                        candidate_map.put(em2, item);
                    }
                } else {
                    candidate_best.put(em2, this.prediction_scores.get(index));
                    candidate_map.put(em2, item);
                }
                index++;
            }

            List<CoreferenceEdge> pred_edges = new ArrayList<CoreferenceEdge>();
            // Before sorting
            for (CoreferenceEdge predicted_positive : candidate_map.values()) {
                pred_edges.add(predicted_positive);
            }

            //sort the candidate edges in each sentence by start offset
            Collections.sort(pred_edges, new Comparator<CoreferenceEdge>() {
                @Override
                public int compare(CoreferenceEdge o1, CoreferenceEdge o2) {
                    // first compare entity mention e1, unless they are the same then look at e2.
                    if (o1.e1.getHeadStartOffset() - o2.e1.getHeadStartOffset() == 0) {
                        return o1.e2.getHeadStartOffset() - o2.e2.getHeadStartOffset();
                    }
                    return o1.e1.getHeadStartOffset() - o2.e1.getHeadStartOffset();
                }
            });

            // After sorting
            if (CorefBaseline.DEBUG) {
                System.out.println("Sorted coreference list");
                for (CoreferenceEdge pp : pred_edges) {
                    System.out.println(pp.e1.getHeadStartOffset() + ":" + pp.e1.getConstituent() + " " + pp.e2.getHeadStartOffset() + ":" + pp.e1.getConstituent());
                }
            }

            // inverted index of mentions
            // then need to assign if its coreferent or not.
            // up to here we don't assume that we know the labels
            ArrayList<List<EntityMention>> equivalence_classes = new ArrayList<List<EntityMention>>();
            for (CoreferenceEdge predicted_positive : pred_edges) {
                // constructing equivalence classes
                Boolean found = false;
                mentions_pair = predicted_positive.getEntityMentions();
                if (mentions_pair.getFirst().getHeadStartOffset() < mentions_pair.getSecond().getHeadStartOffset()) {
                    em1 = mentions_pair.getFirst();
                    em2 = mentions_pair.getSecond();
                } else {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("ERROR: shouldn't have em1 > em2");
                        System.out.println("current example first head: " + mentions_pair.getFirst().getHeadStartOffset() + " first extent: "
                                + mentions_pair.getFirst().getExtentStartOffset() + " second: " + mentions_pair.getSecond().getHeadStartOffset()
                                + " second extent: " + mentions_pair.getSecond().getExtentStartOffset());
                    }
                    em2 = mentions_pair.getFirst();
                    em1 = mentions_pair.getSecond();
                }

                int eq_index = 0;
                for (List<EntityMention> ec : equivalence_classes) {
                    if (ec.contains(em1)) {
                        if (!ec.contains(em2)) {
                            ec.add(em2);
                            if (CorefBaseline.DEBUG) {
                                System.out.println("found [em1] ( " + em1.getHeadStartOffset() + ") adding (" + em2.getHeadStartOffset() + "):" + em2.getConstituent() + " \t to ec: " + eq_index);
                            }
                        }
                        found = true;
                        break;
                    } else if (ec.contains(em2)) {
                        if (!ec.contains(em1)) {
                            ec.add(em1);
                            if (CorefBaseline.DEBUG) {
                                System.out.println("found [em2] ( " + em2.getHeadStartOffset() + ") adding (" + em1.getHeadStartOffset() + "):" + em1.getConstituent() + " \t to ec: " + eq_index);
                            }
                        }
                        found = true;
                        break;
                    }
                    eq_index++;
                }

                if (!found) {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("not found (" + em1.getHeadStartOffset() + "):" + em1.getConstituent() + " or (" + em2.getHeadStartOffset() + "):" + em2.getConstituent());
                    }
                    List<EntityMention> ec = new ArrayList<EntityMention>();
                    ec.add(em1);
                    ec.add(em2);
                    equivalence_classes.add(ec);
                }
            }

            if (CorefBaseline.DEBUG) {
                int len = 0;
                for (List<String> sentence : a.getSentences()) {
                    System.out.print(len + ":");
                    for (String word : sentence) {
                        System.out.print(word + " ");
                    }
                    System.out.println();
                    len += sentence.size();
                }
                System.out.println();
            }

            // add the equivalence classes
            int k = 0;
            for (List<EntityMention> ec : equivalence_classes) {
                if (CorefBaseline.DEBUG) {
                    System.out.print("equivilence classes:");
                    System.out.println("ec:" + k++);
                    for (EntityMention em : ec) {
                        System.out.print(em.getHeadStartOffset() + ": ");
                        for (String word : em.getHead()) {
                            System.out.print(word + " ");
                        }
                        // printing candidate map
                        if (candidate_map.containsKey(em)) {
                            System.out.print("\t\t (" + candidate_map.get(em).e1.getHeadStartOffset() + " " + candidate_map.get(em).e2.getHeadStartOffset() + ")");
                            System.out.print("\t\t score: " + this.prediction_scores.get(index - examples.indexOf(candidate_map.get(em)) - 1));
                        } else {
                            System.out.print("\t\t");
                        }

                        System.out.println("\t\t constituent:" + em.getConstituent());
                    }
                    System.out.println();
                }
                // adding the equivalence classes
                a.addCoreferentEntity(ec);

            }
            // Collect all ACE annotations?
            //
        }
        return candidate_map;
    }


    /**
     * Ultimately we want to build the Coreference graph, here we choose the highest mention pair (a,m) for all mentions m. (over some threshold?)
     * We then measure the precision/recall on these predicted coreference pairs.
     *
     * @return
     */
    public Pair<Map<EntityMention, CoreferenceEdge>, Set<CoreferenceEdge>> stitchGold() {
        int index = 0;
        Map<EntityMention, CoreferenceEdge> candidate_map = null;
        Map<EntityMention, Double> candidate_best = null;
        int ap = 0;
        int tp = 0;
        int fp = 0;
        int jj = 0;

        Set<CoreferenceEdge> positives = new HashSet<>();

        // first need to find all of the possible mapping? and the best mapping
        for (ACEAnnotation a : this.test) {
            // candidate map should be different for each ace annotation
            candidate_map = new HashMap<EntityMention, CoreferenceEdge>();
            candidate_best = new HashMap<EntityMention, Double>();

            // Following standard that we are not relating a later entity mention to an earlier entityMention??
            Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> temp = a.getAllPairsGoldCoreferenceEdges();
            List<CoreferenceEdge> examples = temp.getFirst();

            ap += temp.getFirst().size();
            examples.addAll(temp.getSecond());

            EntityMention em1 = null;
            EntityMention em2 = null;
            Pair<EntityMention, EntityMention> mentions_pair = null;
            for (CoreferenceEdge item : examples) {
                mentions_pair = item.getEntityMentions();
                if (mentions_pair.getFirst().getHeadStartOffset() < mentions_pair.getSecond().getHeadStartOffset()) {
                    em1 = mentions_pair.getFirst();
                    em2 = mentions_pair.getSecond();
                } else {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("ERROR: shouldn't have em1 > em2");
                        System.out.println("current example first head: " + mentions_pair.getFirst().getHeadStartOffset() + " first extent: "
                                + mentions_pair.getFirst().getExtentStartOffset() + " second: " + mentions_pair.getSecond().getHeadStartOffset()
                                + " second extent: " + mentions_pair.getSecond().getExtentStartOffset());
                    }
                    em2 = mentions_pair.getFirst();
                    em1 = mentions_pair.getSecond();
                }

                // threshold
                if (this.prediction_scores.get(index) < CorefBaseline.STHOLD) {
                    //System.out.println(this.prediction_scores.get(index));
                    index++;
                    continue;
                }

                item.score = this.prediction_scores.get(index);
                item.sourceDocument = a;

                if (candidate_best.containsKey(em2)) {
                    if (this.prediction_scores.get(index) > candidate_best.get(em2)) {
                        candidate_best.put(em2, this.prediction_scores.get(index));
                        candidate_map.put(em2, item);
                    }
                } else {
                    candidate_best.put(em2, this.prediction_scores.get(index));
                    candidate_map.put(em2, item);
                }
                index++;
            }

            List<CoreferenceEdge> pred_edges = new ArrayList<CoreferenceEdge>();
            // Before sorting
            for (CoreferenceEdge predicted_positive : candidate_map.values()) {
                pred_edges.add(predicted_positive);
            }

            //sort the candidate edges in each sentence by start offset
            Collections.sort(pred_edges, new Comparator<CoreferenceEdge>() {
                @Override
                public int compare(CoreferenceEdge o1, CoreferenceEdge o2) {
                    // first compare entity mention e1, unless they are the same then look at e2.
                    if (o1.e1.getHeadStartOffset() - o2.e1.getHeadStartOffset() == 0) {
                        return o1.e2.getHeadStartOffset() - o2.e2.getHeadStartOffset();
                    }
                    return o1.e1.getHeadStartOffset() - o2.e1.getHeadStartOffset();
                }
            });

            // After sorting
            if (CorefBaseline.DEBUG) {
                System.out.println("Sorted coreference list");
                for (CoreferenceEdge pp : pred_edges) {
                    System.out.println(pp.e1.getHeadStartOffset() + ":" + pp.e1.getConstituent() + " " + pp.e2.getHeadStartOffset() + ":" + pp.e1.getConstituent());
                }
            }
            // inverted index of mentions
            // then need to assign if its coreferent or not.
            // up to here we don't assume that we know the labels
            ArrayList<List<EntityMention>> equivalence_classes = new ArrayList<List<EntityMention>>();
            for (CoreferenceEdge predicted_positive : pred_edges) {
                positives.add(predicted_positive);

                if (predicted_positive.isCoreferent()) {
                    tp++;
                } else {
                    fp++;
                }

                // constructing equivalence classes
                Boolean found = false;
                mentions_pair = predicted_positive.getEntityMentions();
                if (mentions_pair.getFirst().getHeadStartOffset() < mentions_pair.getSecond().getHeadStartOffset()) {
                    em1 = mentions_pair.getFirst();
                    em2 = mentions_pair.getSecond();
                } else {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("ERROR: shouldn't have em1 > em2");
                        System.out.println("current example first head: " + mentions_pair.getFirst().getHeadStartOffset() + " first extent: "
                                + mentions_pair.getFirst().getExtentStartOffset() + " second: " + mentions_pair.getSecond().getHeadStartOffset()
                                + " second extent: " + mentions_pair.getSecond().getExtentStartOffset());
                    }
                    em2 = mentions_pair.getFirst();
                    em1 = mentions_pair.getSecond();
                }

                int eq_index = 0;
                for (List<EntityMention> ec : equivalence_classes) {
                    if (ec.contains(em1)) {
                        if (!ec.contains(em2)) {
                            ec.add(em2);
                            if (CorefBaseline.DEBUG) {
                                System.out.println("found [em1] ( " + em1.getHeadStartOffset() + ") adding (" + em2.getHeadStartOffset() + "):" + em2.getConstituent() + " \t to ec: " + eq_index);
                            }
                        }
                        found = true;
                        break;
                    } else if (ec.contains(em2)) {
                        if (!ec.contains(em1)) {
                            ec.add(em1);
                            if (CorefBaseline.DEBUG) {
                                System.out.println("found [em2] ( " + em2.getHeadStartOffset() + ") adding (" + em1.getHeadStartOffset() + "):" + em1.getConstituent() + " \t to ec: " + eq_index);
                            }
                        }
                        found = true;
                        break;
                    }
                    eq_index++;
                }

                if (!found) {
                    if (CorefBaseline.DEBUG) {
                        System.out.println("not found (" + em1.getHeadStartOffset() + "):" + em1.getConstituent() + " or (" + em2.getHeadStartOffset() + "):" + em2.getConstituent());
                    }
                    List<EntityMention> ec = new ArrayList<EntityMention>();
                    ec.add(em1);
                    ec.add(em2);
                    equivalence_classes.add(ec);
                }
            }

            if (CorefBaseline.DEBUG) {
                int len = 0;
                for (List<String> sentence : a.getSentences()) {
                    System.out.print(len + ":");
                    for (String word : sentence) {
                        System.out.print(word + " ");
                    }
                    System.out.println();
                    len += sentence.size();
                }
//				System.out.println();
//				
//				for ( EntityMention ttemp :candidate_map.keySet()){
//					System.out.print(ttemp.getHeadStartOffset() + " ");
//					for ( String word: ttemp.getHead()) System.out.print(word + ' ');
//					System.out.println();
//				}
                System.out.println();
            }

            // add the equivalence classes
            int k = 0;
            for (List<EntityMention> ec : equivalence_classes) {
                if (CorefBaseline.DEBUG) {
                    System.out.print("equivilence classes:");
                    System.out.println("ec:" + k++);
                    for (EntityMention em : ec) {
                        System.out.print(em.getHeadStartOffset() + ": ");
                        for (String word : em.getHead()) {
                            System.out.print(word + " ");
                        }
                        // printing candidate map
                        if (candidate_map.containsKey(em)) {
                            System.out.print("\t\t (" + candidate_map.get(em).e1.getHeadStartOffset() + " " + candidate_map.get(em).e2.getHeadStartOffset() + ")");
                            System.out.print("\t\t score: " + this.prediction_scores.get(index - examples.indexOf(candidate_map.get(em)) - 1));
                        } else {
                            System.out.print("\t\t");
                        }

                        System.out.println("\t\t constituent:" + em.getConstituent());
                    }
                    System.out.println();
                }
                //a.addCoreferentEntity(ec);
            }
            // writing to file
            CorefBaseline.write2file(a.getSentences(), equivalence_classes, "test-file-" + jj + ".response", jj);
            jj++;
        }

        this.ap = ap;
        double precision = tp / (double) (tp + fp);
        double recall = tp / (double) ap;
        double f1 = 2 * (precision * recall) / (precision + recall);
        System.out.println("precision:" + precision);
        System.out.println("recall:" + recall);
        System.out.println("f1 score:" + f1);

        return new Pair<>(candidate_map, positives);
    }

    // Predicting with Logistic Regression
    // assumes we do not have label information
    public List<String> predict() throws Exception {
        List<String> predictions = new ArrayList<String>();
        this.prediction_scores = new ArrayList<Double>();
        //System.out.println("predict ...");
        Instances testInstances = FeatureGenerator.readData(this.test, false, true); // assuming we have ground truth label?
        int classIndex = testInstances.numAttributes() - 1;
        testInstances.setClassIndex(classIndex);

        //System.out.println("number of testing instances:" + testInstances.numInstances());
        for (int i = 0; i < testInstances.numInstances(); i++) {
            double predClass = classifier.classifyInstance(testInstances.instance(i));
            String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);
            double[] prediction_distribution = classifier.distributionForInstance(testInstances.instance(i));
            this.prediction_scores.add(prediction_distribution[1]);
            predictions.add(c);
        }
        stitch();
        return predictions;
    }

    // Predicting with Logistic Regression
    public List<String> predictPipeline() throws Exception {
        List<String> predictions = new ArrayList<String>();
        this.prediction_scores = new ArrayList<Double>();
        //System.out.println("predict ...");
        Instances testInstances = FeatureGenerator.readData(this.test, false, false); // assuming we have ground truth label?
        int classIndex = testInstances.numAttributes() - 1;
        testInstances.setClassIndex(classIndex);

        //System.out.println("number of testing instances:" + testInstances.numInstances());
        for (int i = 0; i < testInstances.numInstances(); i++) {
            double predClass = classifier.classifyInstance(testInstances.instance(i));
            String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);
            double[] prediction_distribution = classifier.distributionForInstance(testInstances.instance(i));
            this.prediction_scores.add(prediction_distribution[1]);
            predictions.add(c);
        }
        stitchPipeline();
        return predictions;
    }

    // Predicting Gold with Weka

    /**
     * This method assigns the score to each mention instance, in the testing dataset.
     * Here we used a Pairwise coreference function pc (a,m), which is the output of the logistic regression classifier.
     *
     * @return
     * @throws Exception
     */
    public List<String> predictGold() throws Exception {
        this.prediction_scores = new ArrayList<Double>();
        List<String> predictions = new ArrayList<String>();
        System.out.println("predict ...");
        Instances testInstances = FeatureGenerator.readData(this.test, false, true); // assuming we have ground truth label?
        // saving file
        try {
            File file = new File("arff/estData.arff");

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(testInstances.toString());
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        int classIndex = testInstances.numAttributes() - 1;
        testInstances.setClassIndex(classIndex);
        System.out.println("number of testing instances:" + testInstances.numInstances());
        for (int i = 0; i < testInstances.numInstances(); i++) {
            double predClass = classifier.classifyInstance(testInstances.instance(i));
            double[] prediction_distribution = classifier.distributionForInstance(testInstances.instance(i));
            // i.classValue(); // gives the index of the class index 0 = -1 (False) , index 1 = 1(True)
            // int pred = Integer.parseInt(testInstances.instance(i).attribute(testInstances.instance(i).numAttributes()-1).value((int)predClass));
            String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);

            //System.out.println("prediction distribution label==0:" + prediction_distribution[0] + " label==1" + prediction_distribution[1]);
            this.prediction_scores.add(prediction_distribution[1]);
            // assuming we have the labels
            predictions.add(c);
        }
        stitchGold();
        return predictions;
    }

    public static void write2file(List<List<String>> sentences, List<List<EntityMention>> ec, String outputName, int docId) {
        System.out.println(CorefBaseline.RCSDir + "/" + outputName);

        // word index to equvilance class
        HashMap<Integer, String> mymap = new HashMap<Integer, String>();
        int ec_number = 0;

        // initialize hashmap:
        // is there a better way to do this?
        int index = 0;
        for (List<String> sentence : sentences) {
            for (String word : sentence) {
                mymap.put(index, "-");
                index++;
            }
        }

        // add the code for the hash map?
        for (List<EntityMention> eclass : ec) {
            for (EntityMention em : eclass) {
                String code = "";
                for (int i = em.getHeadStartOffset(); i < em.getHeadEndOffset(); i++) {
                    if (em.getHeadEndOffset() - em.getHeadStartOffset() == 1) {
                        code = "(" + ec_number + ")";
                    } else if (i == em.getHeadStartOffset()) {
                        code = "(" + ec_number;
                    } else if (i == em.getHeadEndOffset() - 1) {
                        code = "" + ec_number + ")";
                    } else {
                        code = "-";
                    }
                    mymap.put(i, code);
                }
            }
            ec_number++;
        }

        int i = 0;
        index = 0;

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(CorefBaseline.RCSDir + "/" + outputName), "utf-8"))) {
            writer.write("#begin document (GoldTestCase);\n");
            for (List<String> sentence : sentences) {
                int j = 0;
                for (String word : sentence) {
                    //System.out.println();
                    writer.write("test" + i + "\t" + docId + "\t" + j + "\t" + word + "\t" + mymap.get(index) + "\n");
                    j++;
                    index++;
                }
                writer.write("\n");
                i++;
            }
            writer.write("#end document\n");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    public static void main(String[] argv) {
        System.out.println("Running CorefBaseline..");

//	     serilazible
//        List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits("./ACE05_English");
//		try {
//            ACEAnnotation.writeAlltoFile(splits);
//        } catch (Exception e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//        System.exit(0);

        // loading processed data
        List<List<ACEAnnotation>> splits = null;
        try {
            splits = ACEAnnotation.readAllFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Done loading splits");
        System.out.flush();
        // temporary training/testing
        ArrayList<ACEAnnotation> train_split = new ArrayList<ACEAnnotation>();
        ArrayList<ACEAnnotation> test_split = new ArrayList<ACEAnnotation>();
        for (int i = 0; i < splits.size() - 1; i++) {
            train_split.addAll(splits.get(i));
        }
        test_split.addAll(splits.get(splits.size() - 1));

        System.out.println("training documents size:" + train_split.size());
        System.out.println("testing documents size:" + test_split.size());

        // Saving the test output
//		for (int i = 0; i < test_split.size(); i++){
//			//System.out.println(train_split.get(i).getGoldCoreferentEntities());
//			if ( test_split.get(i).getGoldCoreferentEntities() != null)
//				CorefBaseline.write2file(test_split.get(i).getSentences(), test_split.get(i).getGoldCoreferentEntities(), "gold-file-"+i+".key", i);
//		}

//		// gold data
//		CorefBaseline cb = new CorefBaseline();
//		cb.trainModel(train_split);
//		cb.test(test_split);

        // pipeline
//		NERBaseline ner = new NERBaseline(true);
//		ner.test(test_split);
//		
//		CorefBaseline cb = new CorefBaseline();
//		cb.trainModel(train_split);
//		cb.testPipeline(test_split);

        // evaluating
        CorefBaseline cb = new CorefBaseline(train_split, test_split);
        cb.learn();
        try {
            cb.predictGold();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
