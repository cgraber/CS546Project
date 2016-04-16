package experiments;

import com.google.common.primitives.Doubles;
import data.ACEAnnotation;
import learn.FeatureVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import utils.Metric;

/**
 * Created by daeyun on 4/1/16.
 */
public class ReSoftmaxBaseline {
    private MultiLayerNetwork model = null;

    public static void main(String[] argv) throws IOException {
        List<ACEAnnotation> aceAnnotations = ACEAnnotation.readAllFromFileFlat();
        List<FeatureVector> featureVectors = REFeatures.generateFeatures(aceAnnotations, "train");

        Collections.shuffle(featureVectors);

        Pair<INDArray, INDArray> featuresAndLabels = matricesFromFeatures(featureVectors);
        INDArray allFeatures = featuresAndLabels.getLeft();
        INDArray allLabels = featuresAndLabels.getRight();

        System.out.println(allFeatures.mean(0));
        System.out.println(allFeatures.std(0));

        int k = 5;
        int n = featureVectors.size();

        // k-fold cross-validation.
        // TODO: repeat multiple times.
        int splitIndex = n / k;

        INDArray trainInput = allFeatures.get(NDArrayIndex.interval(0, splitIndex), NDArrayIndex.all());
        INDArray testInput = allFeatures.get(NDArrayIndex.interval(splitIndex, n), NDArrayIndex.all());
        INDArray trainTarget = allLabels.get(NDArrayIndex.interval(0, splitIndex), NDArrayIndex.all());
        INDArray testTarget = allLabels.get(NDArrayIndex.interval(splitIndex, n), NDArrayIndex.all());

        // Whitening.
        INDArray mean = trainInput.mean(0);
        INDArray std = trainInput.std(0);
        trainInput = trainInput.subRowVector(mean);
        trainInput = trainInput.divRowVector(std.add(1e-2));
        testInput = testInput.subRowVector(mean);
        testInput = testInput.divRowVector(std.add(1e-2));

        System.out.println(trainTarget.sum(0));
        System.out.println(testTarget.sum(0));

        ReSoftmaxBaseline reSoftmaxBaseline = new ReSoftmaxBaseline();

        for (int i = 0; i < 50; i++) {
            reSoftmaxBaseline.train(trainInput, trainTarget);

            Metric trainResult = reSoftmaxBaseline.test(trainInput, trainTarget);
            Metric testResult = reSoftmaxBaseline.test(testInput, testTarget);

            System.out.println("Train: " + trainResult.toString());
            System.out.println("Test:  " + testResult.toString());
        }
    }

    public static Pair<INDArray, INDArray> matricesFromFeatures(List<FeatureVector> features) {
        int featureSize = features.get(0).getFeatureCount();
        int labelSize = features.get(0).getLabelCount();

        List<Integer> allFeatures = new ArrayList<>();
        List<Integer> allLabels = new ArrayList<>();
        for (FeatureVector feature : features) {
            allFeatures.addAll(new ArrayList<>(feature.getFeatures()));
            allLabels.addAll(new ArrayList<>(feature.getLabelVector()));
        }

        double[] featureArray = Doubles.toArray(allFeatures);
        double[] labelArray = Doubles.toArray(allLabels);
        INDArray featureBuffer = Nd4j.create(featureArray);
        INDArray labelBuffer = Nd4j.create(labelArray);
        featureBuffer = featureBuffer.reshape(features.size(), featureSize);
        labelBuffer = labelBuffer.reshape(features.size(), labelSize);

        return Pair.of(featureBuffer, labelBuffer);
    }

    private void train(INDArray input, INDArray target) {
        if (model == null) {
            int featureSize = input.size(1);
            int labelSize = target.size(1);

            // Multinomial logistic regression (no hidden layer).
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .iterations(20).learningRate(0.005).regularization(true).l1(3)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .updater(Updater.NESTEROVS).momentum(0.999).list(1)
                    .layer(0, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                            .weightInit(WeightInit.XAVIER)
                            .activation("softmax").nIn(featureSize).nOut(labelSize).build())
                    .pretrain(false).backprop(true).build();

            model = new MultiLayerNetwork(conf);
            model.init();
            System.out.println("Initialized a new model.");
        }

        model.fit(input, target);
    }

    private Metric test(INDArray input, INDArray target) {
        INDArray output = model.output(input);
        INDArray predictions = Nd4j.argMax(output, 1);
        INDArray labels = Nd4j.argMax(target, 1);
        INDArray isCorrect = predictions.eq(labels);

        int numLabels = target.size(1);
        double[] tp = new double[numLabels];
        double[] tn = new double[numLabels];
        double[] fp = new double[numLabels];
        double[] fn = new double[numLabels];

        for (int i = 0; i < predictions.length(); i++) {
            if (isCorrect.getInt(i) == 1) {
                for (int j = 0; j < numLabels; j++) {
                    if (predictions.getInt(i) == j) {
                        tp[j]++;
                    } else {
                        tn[j]++;
                    }
                }
            } else {
                for (int j = 0; j < numLabels; j++) {
                    if (predictions.getInt(i) == j) {
                        fp[j]++;
                    } else if (labels.getInt(i) == j) {
                        fn[j]++;
                    } else {
                        tn[j]++;
                    }
                }
            }
        }

        double precision = 0, recall = 0;

        for (int i = 0; i < numLabels; i++) {
            precision += tp[i] / (tp[i] + fp[i]);
            recall += tp[i] / (tp[i] + fn[i]);
        }
        precision /= numLabels;
        recall /= numLabels;

        double accuracy = isCorrect.sumNumber().doubleValue() / isCorrect.length();

        Metric metric = new Metric();
        metric.setAccuracy(accuracy);
        metric.setPrecision(precision);
        metric.setRecall(recall);

        return metric;
    }
}
