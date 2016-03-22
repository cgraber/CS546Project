package experiments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import weka.core.Instances;
import learn.FeatureGenerator;
import learn.PipelineStage;
import data.ACEAnnotation;
import data.CoreferenceEdge;
import data.DataUtils;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.lbjava.learn.NaiveBayes;
import weka.classifiers.functions.Logistic;

/**
 * The baseline coreference class using simple features and binary classification setup.
 *
 * Created by Alex Morales on 3/21/16.
 */
public class CorefBaseline implements PipelineStage{
	private ArrayList<ACEAnnotation> train;
	private ArrayList<ACEAnnotation> test;
	
	//Weka
	private Logistic classifier;
	
	
	public CorefBaseline(){
		this.train = null;
		this.test = null;
		this.classifier = null;
	}
	
	public CorefBaseline(List<List<ACEAnnotation>> train){
		this.train = new ArrayList<ACEAnnotation> ();
		for (List<ACEAnnotation> item : train){
			this.train.addAll(item);
		}
		this.classifier = new Logistic();
	}
	public CorefBaseline(List<List<ACEAnnotation>> train, List<List<ACEAnnotation>> test){
		this.train = new ArrayList<ACEAnnotation> ();
		for (List<ACEAnnotation> item : train){
			this.train.addAll(item);
		}
		
		this.test = new ArrayList<ACEAnnotation> ();
		for (List<ACEAnnotation> item : train){
			this.test.addAll(item);
		}
		this.classifier = new Logistic();
	}
	
	public CorefBaseline(ArrayList<ACEAnnotation> train, ArrayList<ACEAnnotation> test){
		this.train = new ArrayList<ACEAnnotation>();
		this.train.addAll(train);
		this.test = new ArrayList<ACEAnnotation>();
		this.test.addAll(test);
		this.classifier = new Logistic();
	}


	@Override
	public void trainModel(List<ACEAnnotation> data) {
		// TODO Auto-generated method stub
		this.train = new ArrayList<ACEAnnotation>();
		this.train.addAll(data);
		
		this.learn();
	}

	@Override
	public void test(List<ACEAnnotation> data) {
		// TODO Auto-generated method stub
		this.test = new ArrayList<ACEAnnotation>();
		this.test.addAll(data);
		
		try {
			this.predict();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Learning with WEKA
	private void learn(){
		Instances trainInstances = FeatureGenerator.readData(this.train, true);
		trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
		System.out.println("number of training instances:" + trainInstances.numInstances());
		System.out.println("building classifier");
		try {
			classifier.buildClassifier(trainInstances);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("Unable to build Classifier...\n");
			e.printStackTrace();
		}
			
	}
		
	// Predicting with Weka
	private void predict() throws Exception{
		Instances testInstances = FeatureGenerator.readData(this.test, true); // assuming we have ground truth label?
		testInstances.setClassIndex(testInstances.numAttributes() - 1);
		System.out.println("number of testing instances:" + testInstances.numInstances());
		for (int i = 0; i < testInstances.numInstances(); i++){
			double predClass = classifier.classifyInstance(testInstances.instance(i));
			//i.classValue(); // gives the index of the class index 0 = -1 (False) , index 1 = 1(True)
			int pred = Integer.parseInt(testInstances.instance(i).attribute(testInstances.instance(i).numAttributes()-1).value((int)predClass));
			if (pred > 0)
				System.out.println("predicted class: " + pred + " actual class (need to get?): ");
		}
	}
	
	public static void main( String[] argv ){
		List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits(argv[0]);
		
		// temporary training/testing
		ArrayList<ACEAnnotation> train_split = new ArrayList<ACEAnnotation>();
		ArrayList<ACEAnnotation> test_split = new ArrayList<ACEAnnotation>();
		for (int i = 0; i < splits.size()-1; i++){
			train_split.addAll( splits.get(i));
		}
		test_split.addAll(splits.get(splits.size()-1));
		
		System.out.println(train_split.size());
		System.out.println(test_split.size());
		
		CorefBaseline cb = new CorefBaseline(train_split, test_split);
		
		cb.learn();
		
		try {
			cb.predict();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
