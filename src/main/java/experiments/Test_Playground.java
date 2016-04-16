package experiments;

import data.*;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.lbjava.nlp.StringArraysToWords;
import org.deeplearning4j.datasets.iterator.impl.IrisDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.layers.factory.LayerFactories;
import org.deeplearning4j.nn.params.DefaultParamInitializer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.log;

/**
 * Created by sdq on 3/23/16.
 */
public class Test_Playground {



    public static void main(String[] args) throws IOException {

        List<ACEAnnotation> test=new ArrayList<>();
        test.add(ACEAnnotation.readFileByID(0));

        List<GISentence> sentences = GISentence.BreakDocumentIntoSentence(test, 1);

        /*
        ACEAnnotation doc=test.get(0);
        Map<Pair<EntityMention,EntityMention>, CoreferenceEdge> map = doc.getGoldCoreferenceEdgesByEntities();
        System.out.println(map.size());
        */

    }



}
