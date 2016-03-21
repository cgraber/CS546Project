package experiments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import learn.PipelineStage;
import data.ACEAnnotation;
import data.CoreferenceEdge;
import data.DataUtils;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;


/**
 * The baseline coreference class using simple features and binary classification setup.
 *
 * Created by Alex Morales on 3/21/16.
 */
public class CorefBaseline implements PipelineStage{
	private ArrayList<ACEAnnotation> train;
	private ArrayList<ACEAnnotation> test;
	
	
	public CorefBaseline(){
		train = null;
		test = null;
	}
	
	public CorefBaseline(List<List<ACEAnnotation>> train){
		this.train = new ArrayList<ACEAnnotation> ();
		for (List<ACEAnnotation> item : train){
			this.train.addAll(item);
		}
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
	}
	
	public CorefBaseline(ArrayList<ACEAnnotation> train, ArrayList<ACEAnnotation> test){
		this.train = new ArrayList<ACEAnnotation>();
		this.train.addAll(train);
		this.test = new ArrayList<ACEAnnotation>();
		this.test.addAll(test);
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
		
	}

	// main method in charge of learning the model
	private void learn(){
		for (ACEAnnotation entry : this.train){
			Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> mymap = entry.getAllPairsGoldCoreferenceEdges();
			List<CoreferenceEdge> myGCE = entry.getGoldCoreferenceEdges();
			
			System.out.println("current instance number of sentences:" + entry.getNumberOfSentences());
			for (List<String> sentences : entry.getSentences()){
				System.out.println(sentences);
			}
			for (CoreferenceEdge ce : myGCE){
				System.out.println(ce.getEntityMentions());
			}
			break;
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
		//Set<String> relationtypes = ACEAnnotation.getRelationTypes();
		//Set<String> entitytypes = ACEAnnotation.getEntityTypes();
		
		//for (String relationtype : relationtypes){
		//	System.out.println(relationtype);
		//}
		
	}

}
