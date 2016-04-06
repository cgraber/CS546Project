package experiments;

import java.util.ArrayList;
import java.util.HashMap;
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
	//private Map<EntityMention, List<CoreferenceEdge>> candidate_map;
	List<Double> prediction_scores;
	
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
	
	
	// assuming gold data for train?
	public CorefBaseline(ArrayList<ACEAnnotation> train, ArrayList<ACEAnnotation> test){
		this.train = new ArrayList<ACEAnnotation>();
		this.train.addAll(train);
		addCandidates(train);
		this.test = new ArrayList<ACEAnnotation>();
		this.test.addAll(test);
		this.classifier = new Logistic();
	}

	public void addCandidates(ArrayList<ACEAnnotation> data){;
		for (ACEAnnotation entry: data){
			Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> temp = entry.getAllPairsGoldCoreferenceEdges();
			List<CoreferenceEdge> positive_examples = temp.getFirst();
		}
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
		this.prediction_scores = new ArrayList<Double>();
		System.out.println("predict ...");
		Instances testInstances = FeatureGenerator.readData(this.test, false, false); // assuming we have ground truth label?
		int classIndex = testInstances.numAttributes() - 1;
		testInstances.setClassIndex(classIndex);
		
		System.out.println("number of testing instances:" + testInstances.numInstances());
		for (int i = 0; i < testInstances.numInstances(); i++){
			double predClass = classifier.classifyInstance(testInstances.instance(i));
			String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);
			double[] prediction_distribution =  classifier.distributionForInstance(testInstances.instance(i));
			this.prediction_scores.add(prediction_distribution[1]);
			predictions.add(c);
		}
		stitch();
		return predictions;
	}
	
	private Map<EntityMention, CoreferenceEdge> stitch(){
		int index = 0;
		Map <EntityMention, CoreferenceEdge > candidate_map = new HashMap<EntityMention, CoreferenceEdge>();
		Map <EntityMention, Double > candidate_best = new HashMap<EntityMention, Double>();
		
		// first need to find all of the possible mapping? and the best mapping
		for (ACEAnnotation a : this.test){
			// Following standard that we are not relating a later entity mention to an earlier entityMention??
			List<CoreferenceEdge> examples = a.getAllPairsPipelineCoreferenceEdges();
		
			//ap += temp.getFirst().size();
			
			EntityMention em2 = null;
			
			for ( CoreferenceEdge item : examples){
				Pair<EntityMention, EntityMention> mentions_pair = item.getEntityMentions();
				if (mentions_pair.getFirst().getStartOffset() < mentions_pair.getSecond().getStartOffset() ){
					em2 = mentions_pair.getSecond();
				}
				else{
					em2 = mentions_pair.getFirst();
				}
				
				if( candidate_best.containsKey(em2) ){
					if ( this.prediction_scores.get(index) > candidate_best.get(em2)){
						candidate_best.put(em2, this.prediction_scores.get(index));
						candidate_map.put(em2, item);
					}
				}else{
					candidate_best.put(em2, this.prediction_scores.get(index));
					candidate_map.put(em2, item);
				}
				index++;
			}
		}
		return candidate_map;
	}
	
	
	/**
	 * Ultimately we want to build the Coreference graph, here we choose the highest mention pair (a,m) for all mentions m. (over some threshold?)
	 * We then measure the precision/recall on these predicted coreference pairs. 
	 * @return
	 */
	private Map<EntityMention, CoreferenceEdge> stitchGold(){
		int index = 0;
		Map <EntityMention, CoreferenceEdge > candidate_map = new HashMap<EntityMention, CoreferenceEdge>();
		Map <EntityMention, Double > candidate_best = new HashMap<EntityMention, Double>();
		int ap = 0;
		//int an = 0;
		
		// first need to find all of the possible mapping? and the best mapping
		for (ACEAnnotation a : this.test){
			// Following standard that we are not relating a later entity mention to an earlier entityMention??
			Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> temp = a.getAllPairsGoldCoreferenceEdges();
			List<CoreferenceEdge> examples = temp.getFirst();
			
//			for(CoreferenceEdge edge: temp.getFirst()){
//				if (edge.isCoreferent() == false){
//					System.out.println("should only be true: " + edge.isCoreferent());
//					break;
//				}
//			}
			
			ap += temp.getFirst().size();
			//an += temp.getSecond().size();
			examples.addAll(temp.getSecond());
			
//			for(CoreferenceEdge edge: temp.getSecond() ){
//				if (edge.isCoreferent() == true){
//					System.out.println("should only be false: " + edge.isCoreferent());
//					break;
//				}
//			}
			EntityMention em1 = null;
			EntityMention em2 = null;
			
			for ( CoreferenceEdge item : examples){
				Pair<EntityMention, EntityMention> mentions_pair = item.getEntityMentions();
				if (mentions_pair.getFirst().getStartOffset() < mentions_pair.getSecond().getStartOffset() ){
					em1 = mentions_pair.getFirst();
					em2 = mentions_pair.getSecond();
				}
				else{
					em2 = mentions_pair.getFirst();
					em1 = mentions_pair.getSecond();
				}
				//System.out.println("first ("+em1.getStartOffset() +"-"+ em1.getEndOffset()+"):"+ em1.getExtent() + " second ("+em2.getStartOffset() +"-"+ em2.getEndOffset()+"):" + em2.getExtent());
//				if( positive_examples.contains(  )  ){
//					System.out.println("contains duplicate");
//				}
				//candidate_map.put(em2, item);
				
				if( candidate_best.containsKey(em2) ){
					if ( this.prediction_scores.get(index) > candidate_best.get(em2)){
						candidate_best.put(em2, this.prediction_scores.get(index));
						candidate_map.put(em2, item);
					}
				}else{
					candidate_best.put(em2, this.prediction_scores.get(index));
					candidate_map.put(em2, item);
				}
				index++;
			}
		}
		// up to here we don't assume that we know the labels
		
		
		int tp = 0;
		int fp = 0;
		for ( CoreferenceEdge predicted_positive: candidate_map.values()){
			//Pair<EntityMention, EntityMention> temp = predicted_positive.getEntityMentions();
			//EntityMention em1 = temp.getFirst();
			//EntityMention em2 = temp.getSecond();
			//System.out.println("em1:" + em1.getExtent() + " em2:" + em2.getExtent());
			if ( predicted_positive.isCoreferent() ){
				tp++;
			} else{
				fp++;
			}
		}
		double precision = tp/(double)(tp+fp);
		double recall = tp / (double)ap;
		double f1 = 2*(precision*recall)/(precision+ recall);
		System.out.println("precision:" + precision);
		System.out.println("recall:" + recall);
		System.out.println("f1 score:" + f1);
		
		return candidate_map;
		// then need to assign if its corefferent or not.
	}
	
	
	
	
	// Predicting with Weka
	/**
	 * This method assigns the score to each mention instance, in the testing dataset. 
	 * Here we used a Pairwise coreference function pc (a,m), which is the output of the logistic regression classifier. 
	 * @return
	 * @throws Exception
	 */
	private List<String> predictGold() throws Exception{
		this.prediction_scores = new ArrayList<Double>();
		List<String> predictions = new ArrayList<String> ();
		System.out.println("predict ...");
		Instances testInstances = FeatureGenerator.readData(this.test, false, true); // assuming we have ground truth label?
		int classIndex = testInstances.numAttributes() - 1;
		testInstances.setClassIndex(classIndex);
//		int ap = 0;
//		int an = 0;
//		int tp = 0;
//		int tn = 0;
//		int fp = 0;
//		int fn = 0;
		System.out.println("number of testing instances:" + testInstances.numInstances());
		for (int i = 0; i < testInstances.numInstances(); i++){
			double predClass = classifier.classifyInstance(testInstances.instance(i));
			double[] prediction_distribution =  classifier.distributionForInstance(testInstances.instance(i));
			// i.classValue(); // gives the index of the class index 0 = -1 (False) , index 1 = 1(True)
			// int pred = Integer.parseInt(testInstances.instance(i).attribute(testInstances.instance(i).numAttributes()-1).value((int)predClass));
			String c = testInstances.instance(i).attribute(classIndex).value((int) predClass);
			
			int pred = Integer.parseInt(c);
			//double max_prob = 0;;
//			for(int j=0; j<prediction_distribution.length; j++)
//	        {
//				if ( prediction_distribution[j] > max_prob ){
//					max_prob = prediction_distribution[j];
//				}
//	        }
			//this.prediction_scores.add(max_prob);'
			// P(label == 1 )
			this.prediction_scores.add(prediction_distribution[1]);
			
			// assuming we have the labels
			//int actual = Integer.parseInt( FeatureGenerator.testLabels.get(i));
			//System.out.println("predicted class: " + pred + " actual class: " + actual);
//			if (actual > 0){
//				if (pred > 0){
//					tp = tp + 1;
//				} else{
//					fn = fn + 1;
//				}
//				ap = ap + 1;
//			}
//			else{
//				if (pred < 0){
//					//System.out.println("predicted class: " + pred + " actual class: " + actual);
//					tn = tn + 1;
//				} else {
//					fp = fp + 1;
//				}
//				an = an + 1;
//			}
			//predictions.add((double)pred);
			predictions.add(c);
			//break;
		}
//		double precision = (tp/(double)(fp + tp));
//		double recall = (tp/(double)ap);
//		System.out.println(" true positive: " + tp);
//		System.out.println(" true negative: " + tn);
//		System.out.println(" false positive: " + fp);
//		System.out.println(" false negative: " + fn);
//		System.out.println(" actual positive: " + ap);
//		System.out.println(" actual negative: " + an);
//		System.out.println("precision: " + precision );		
//		System.out.println("recall: " + recall );
//		System.out.println("F1 score: " + (2*precision*recall/ (precision + recall)) );
//		System.out.println("specificity: " + (tn/(double)an) );
//		System.out.println("accuracy:" + (tp + tn)/(double)(ap + an));
//		
//		Map<String,List<Double>> map = new HashMap<String, List<Double>>();
//		map.put("predictions", predictions);
//		map.put("probability", prediction_scores);
		stitchGold();
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
