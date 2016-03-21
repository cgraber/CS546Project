package experiments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import learn.PipelineStage;
import data.ACEAnnotation;
import data.CoreferenceEdge;
import data.DataUtils;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.lbjava.learn.NaiveBayes;


/**
 * The baseline coreference class using simple features and binary classification setup.
 *
 * Created by Alex Morales on 3/21/16.
 */
public class CorefBaseline implements PipelineStage{
	private ArrayList<ACEAnnotation> train;
	private ArrayList<ACEAnnotation> test;
	private NaiveBayes nb;
	
	public CorefBaseline(){
		this.train = null;
		this.test = null;
		this.nb = null;
	}
	
	public CorefBaseline(List<List<ACEAnnotation>> train){
		this.train = new ArrayList<ACEAnnotation> ();
		for (List<ACEAnnotation> item : train){
			this.train.addAll(item);
		}
		this.nb = new NaiveBayes();
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
		this.nb = new NaiveBayes();
	}
	
	public CorefBaseline(ArrayList<ACEAnnotation> train, ArrayList<ACEAnnotation> test){
		this.train = new ArrayList<ACEAnnotation>();
		this.train.addAll(train);
		this.test = new ArrayList<ACEAnnotation>();
		this.test.addAll(test);
		this.nb = new NaiveBayes();
		this.nb.forget();
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
		
		this.predict();
	}
	
	
	// main method incharge of predicting the label for new instances. 
	private void predict() {
		System.out.println("predicting");
		int num_features = 2;
		int[] exampleFeatures =  new int [num_features];
		exampleFeatures[0] = 0; // distance features
		exampleFeatures[1] = 1; // other?
		double[] exampleValues = null; // construct these
		
		for (ACEAnnotation entry : this.test){
			Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> myLabels = entry.getAllPairsGoldCoreferenceEdges();
			System.out.println("current instance number of sentences:" + entry.getNumberOfSentences());
			for (List<String> sentences : entry.getSentences()){
				System.out.println(sentences);
			}
			
			// Positive Labels are from the gold annotation labels
			// Positive Labels only
			for (CoreferenceEdge ce : myLabels.getFirst()){
				Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
				exampleValues = construct_features( somePair, num_features);
				System.out.println("features:");
				for (double value : exampleValues){
					System.out.println(value);
				}
				System.out.println("comparing: " + somePair.getFirst().getExtent() + " with: " + somePair.getSecond().getExtent());
				
				System.out.println("predicted: " +  this.nb.featureValue(exampleFeatures, exampleValues) + " isCoref label: " + ce.isCoreferent());
				System.out.println("scores: " +  this.nb.scores(exampleFeatures, exampleValues) );
				break;
			}
			
			// Negative Labels need to be constructed ?
//			for (CoreferenceEdge ce : myLabels.getSecond()){
//				Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
//				exampleValues = construct_features( somePair, num_features);
//				
//				System.out.println("predicted:" +  nb.discreteValue(exampleFeatures, exampleValues) + " true: " + ce.isCoreferent());
//			}
		}
		
	}
	
	// main method in charge of learning the model
	private void learn(){
		System.out.println("learning...");
		int num_features = 2;
		int[] exampleFeatures =  new int [num_features];
		exampleFeatures[0] = 0; // distance features
		exampleFeatures[1] = 1; // other?
		int[] exampleLabels = new int [2]; 
		exampleLabels[0] = 0; // not coreferent
		exampleLabels[1] = 1; // coreferent.
		double[] exampleValues = null; // construct these
		double[] labelValues = new double [2];
		
		for (ACEAnnotation entry : this.train){
			Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> myLabels = entry.getAllPairsGoldCoreferenceEdges();
			//System.out.println("current instance number of sentences:" + entry.getNumberOfSentences());
			//for (List<String> sentences : entry.getSentences()){
			//	System.out.println(sentences);
			//}
			
			// Positive Labels are from the gold annotation labels
			// Positive Labels only
			for (CoreferenceEdge ce : myLabels.getFirst()){
				Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
				exampleValues = construct_features( somePair, num_features);
				exampleLabels[0] = 1;
				exampleLabels[1] = 0;
				if (ce.isCoreferent()){
					exampleLabels[0] = 0;
					exampleLabels[1] = 1;
					//System.out.println("correferent pair" +  somePair.getFirst().getExtent() + " " + somePair.getSecond().getExtent());
				}
				//}else{
				//	System.out.println("not correferent pair" +  somePair.getFirst().getExtent() + " " + somePair.getSecond().getExtent());
				//}
				this.nb.learn(exampleFeatures, exampleValues, exampleLabels, labelValues);
			}
			
			// Negative Labels need to be constructed ?
			for (CoreferenceEdge ce : myLabels.getSecond()){
				Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
				exampleValues = construct_features( somePair, num_features);
				exampleLabels[0] = 1;
				exampleLabels[1] = 0;
				if (ce.isCoreferent()){
					exampleLabels[0] = 0;
					exampleLabels[1] = 1;
					//System.out.println("correferent pair" +  somePair.getFirst().getExtent() + " " + somePair.getSecond().getExtent());
				}
				//else{
				//	System.out.println("not correferent pair" +  somePair.getFirst().getExtent() + " " + somePair.getSecond().getExtent());
				//}
				this.nb.learn(exampleFeatures, exampleValues, exampleLabels, labelValues);
			}
		}
		
		for (String value: this.nb.allowableValues()){
			System.out.println(value);
		}
		
	}
	
	private double[] construct_features(Pair<EntityMention, EntityMention> somePair, int feature_size){
		double[] ret = new double[feature_size];
		double distance = CorefBaseline.distanceFeature(somePair.getFirst(), somePair.getSecond());
		ret[0] = distance;
		ret[1] = somePair.getFirst().getType() == somePair.getSecond().getType()? 1 : 0;
		return ret;	
	}
	
	/**
	 * Distance between two mentions.
	 * @param e1 is the left most mention
	 * @param e2 is the rightmost mention.
	 */
	public static double distanceFeature(EntityMention e1, EntityMention e2){
		//System.out.println("entity1 location:" + e1.getStartOffset()+ " to: " + e1.getEndOffset());
		//System.out.println("entity2 location:" + e2.getStartOffset()+ " to: " + e2.getEndOffset());
		//System.out.println("entity1:" + e1.getExtent() + " could be coreferent to " + " entity2: " + e2.getExtent() );
		return Math.abs(e1.getEndOffset() - e2.getStartOffset());
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
		cb.predict();
		//Set<String> relationtypes = ACEAnnotation.getRelationTypes();
		//Set<String> entitytypes = ACEAnnotation.getEntityTypes();
		
		//for (String relationtype : relationtypes){
		//	System.out.println(relationtype);
		//}
		
	}
}
