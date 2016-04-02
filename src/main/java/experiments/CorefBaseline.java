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
		Instances trainInstances = FeatureGenerator.readData(this.train, true, true);
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
	private List<String> predict() throws Exception{
		List<String> predictions = new ArrayList<String> ();
		System.out.println("predict ...");
		Instances testInstances = FeatureGenerator.readData(this.test, false, false); // assuming we have ground truth label?
		int classIndex = testInstances.numAttributes() - 1;
		testInstances.setClassIndex(classIndex);
		
		System.out.println("number of testing instances:" + testInstances.numInstances());
		for (int i = 0; i < testInstances.numInstances(); i++){
			double predClass = classifier.classifyInstance(testInstances.instance(i));
			String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);
			predictions.add(c);
		}
		
		return predictions;
	}
	
	// Predicting with Weka
	private List<String> predictGold() throws Exception{
		List<String> predictions = new ArrayList<String> ();
		System.out.println("predict ...");
		Instances testInstances = FeatureGenerator.readData(this.test, false, true); // assuming we have ground truth label?
		int classIndex = testInstances.numAttributes() - 1;
		testInstances.setClassIndex(classIndex);
		int ap = 0;
		int an = 0;
		int tp = 0;
		int tn = 0;
		int fp = 0;
		int fn = 0;
		System.out.println("number of testing instances:" + testInstances.numInstances());
		for (int i = 0; i < testInstances.numInstances(); i++){
			double predClass = classifier.classifyInstance(testInstances.instance(i));
			// i.classValue(); // gives the index of the class index 0 = -1 (False) , index 1 = 1(True)
			// int pred = Integer.parseInt(testInstances.instance(i).attribute(testInstances.instance(i).numAttributes()-1).value((int)predClass));
			String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);
			int pred = Integer.parseInt(c);
			
			// assuming we have the labels
			int actual = Integer.parseInt( FeatureGenerator.testLabels.get(i));
			//System.out.println("predicted class: " + pred + " actual class: " + actual);
			if (actual > 0){
				if (pred > 0){
					tp = tp + 1;
				} else{
					fn = fn + 1;
				}
				ap = ap + 1;
			}
			else{
				if (pred < 0){
					//System.out.println("predicted class: " + pred + " actual class: " + actual);
					tn = tn + 1;
				} else {
					fp = fp + 1;
				}
				an = an + 1;
			}
			predictions.add(c);
			// TODO: remove
			//break;
		}
		double precision = (tp/(double)(fp + tp));
		double recall = (tp/(double)ap);
		System.out.println(" true positive: " + tp);
		System.out.println(" true negative: " + tn);
		System.out.println(" false positive: " + fp);
		System.out.println(" false negative: " + fn);
		System.out.println(" actual positive: " + ap);
		System.out.println(" actual negative: " + an);
		System.out.println("precision: " + precision );		
		System.out.println("recall: " + recall );
		System.out.println("F1 score: " + (2*precision*recall/ (precision + recall)) );
		System.out.println("specificity: " + (tn/(double)an) );
		System.out.println("accuracy:" + (tp + tn)/(double)(ap + an));

		
		return predictions;
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
		
		System.out.println("training documents size:" + train_split.size());
		System.out.println("testing documents size" + test_split.size());
		
		CorefBaseline cb = new CorefBaseline(train_split, test_split);
		
		cb.learn();
//		
		try {
			cb.predictGold();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
