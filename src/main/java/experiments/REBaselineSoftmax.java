package experiments;

import com.google.common.primitives.Doubles;
import data.ACEAnnotation;
import learn.FeatureVector;

import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;
import org.nd4j.linalg.dataset.DataSet;
import org.deeplearning4j.datasets.iterator.DataSetIterator;

/**
 * Created by daeyun on 4/1/16.
 */
public class REBaselineSoftmax {
    public static void main(String[] argv) throws IOException {
        List<FeatureVector> features = RelationExtraction.FeaturesGenerator();
        // TODO(daeyun): train/test split

        int numFeatures = features.get(0).getFeatures().size();
        int numLabels = features.get(0).getLabelCount();

        List<Integer> allFeatures = new ArrayList<>();
        List<Integer> allLabels = new ArrayList<>();
        for (FeatureVector feature : features) {
            System.out.println(feature.getFeatures().size());
//            assert feature.getFeatures().size() == numFeatures;
//            assert feature.getLabelCount() == numLabels;
            allFeatures.addAll(new ArrayList<>(feature.getFeatures()));
            allLabels.addAll(new ArrayList<>(feature.getOneHotLabel()));
        }

        double[] featureArray = Doubles.toArray(allFeatures);
        double[] labelArray = Doubles.toArray(allLabels);
        INDArray featureBuffer = Nd4j.create(featureArray);
        INDArray labelBuffer = Nd4j.create(labelArray);
        featureBuffer.reshape(features.size(), numFeatures);
        labelBuffer.reshape(features.size(), numLabels);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(42)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(0.01)
                .updater(Updater.NESTEROVS).momentum(0.9)
                .list(1)
                .layer(0, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
                        .weightInit(WeightInit.XAVIER)
                        .activation("softmax").weightInit(WeightInit.XAVIER)
                        .nIn(numFeatures).nOut(numLabels).build())
                .pretrain(false).backprop(true).build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        // Train
        model.fit(featureBuffer, labelBuffer);

        // TODO(daeyun): evaluate

    }
}
