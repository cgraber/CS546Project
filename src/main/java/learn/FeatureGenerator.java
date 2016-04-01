package learn;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.lang.Integer;

import data.ACEAnnotation;
import data.CoreferenceEdge;
import data.EntityMention;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class FeatureGenerator {

    static String[] one_hot_features;
    static String[] two_hot_features;
    private static java.util.Map<String,Attribute> attribute_dict = new java.util.HashMap<String,Attribute>();
    private static FastVector attributes;
    private static Attribute classLabel;	
    private static FastVector zeroOne;
    private static FastVector labels;
    public static List<String> testLabels;
    private static final boolean LOCATIONFEATURES=false;
    private static final boolean MENTIONFEATURES=true;
    private static final boolean STRINGFEATURES=false;
    private static final boolean SEMANTICFEATURES=false;
    ///public static final int NUM_CHARS_PER_NAME = 5;

    static {
    	testLabels = new ArrayList<String>();
    	// used for binary features ( when constructing new attributes: new Attribute(featureName, zeroOne);)
    	zeroOne = new FastVector(2);
		zeroOne.addElement("1"); // yes
		zeroOne.addElement("0"); // no
		
		labels = new FastVector(2);
		labels.addElement("-1");
		labels.addElement("1");
	
		//Create one-hot features for the first five characters in both first and last name.
		attributes = new FastVector();	
		String attribute_name;
		Attribute a;
		
		if(FeatureGenerator.MENTIONFEATURES){
			String attr_name_prefix = "mentionsType1-";
			for (String entityType: ACEAnnotation.getMentionTypes()){
				System.out.println(attr_name_prefix + entityType);
				a = new Attribute(attr_name_prefix + entityType, zeroOne);
				attribute_dict.put(attr_name_prefix + entityType, a);
				attributes.addElement(a);
			}
			
			attr_name_prefix = "mentionsType2-";
			for (String entityType: ACEAnnotation.getMentionTypes()){
				System.out.println(attr_name_prefix + entityType);
				a = new Attribute(attr_name_prefix + entityType, zeroOne);
				attribute_dict.put(attr_name_prefix + entityType, a);
				attributes.addElement(a);
			}
			
			attr_name_prefix = "mentionTypePAIR-";
			for (String entityType: ACEAnnotation.getMentionTypes()){
				a = new Attribute(attr_name_prefix + entityType, zeroOne);
				System.out.println(attr_name_prefix + entityType);
				attribute_dict.put(attr_name_prefix + entityType, a);
				attributes.addElement(a);
			}
//			a = new Attribute(attribute_name, zeroOne);
//			attribute_dict.put(attribute_name, a);
//			attributes.addElement(a);
		}
		
		if (FeatureGenerator.STRINGFEATURES){
			// same extents
			attribute_name = "extentMatch";
			a = new Attribute(attribute_name, zeroOne);
			attribute_dict.put(attribute_name, a);
			attributes.addElement(a);
			
			// one extent substring of another
			attribute_name = "extentSubstring";
			a = new Attribute(attribute_name, zeroOne);
			attribute_dict.put(attribute_name, a);
			attributes.addElement(a);
		}
		
		if (FeatureGenerator.SEMANTICFEATURES){
			// need to set before?
			System.setProperty("wordnet.database.dir", "/usr/local/WordNet-3.0/dict/");
			// gender
			// number match
			// wordnet features
			// modifiers match
			// both mentions speak
		}
		
		//generic feature construction
		if(FeatureGenerator.LOCATIONFEATURES){
			attribute_name = "mentionDistances";
			//String FeatureType = "numeric";
			a = new Attribute(attribute_name);
			attribute_dict.put(attribute_name, a);
			attributes.addElement(a);
			
			// a position?
		}
		
		//Leave the class as the last attribute, though not strictly neccessary.
		classLabel = new Attribute("Class", labels);
		attribute_dict.put("Class",classLabel);
		attributes.addElement(classLabel);

    }

    public static Instances readData(ArrayList<ACEAnnotation> data, Boolean labeled, Boolean gold){
    	Instances instances = initializeAttributes();
    	ArrayList<Instance> docInstances = null;
    	for (ACEAnnotation entry : data){
    		//if (labeled){
    		docInstances = getDocInstance(instances, entry, labeled, gold);
//    		}
//    		else{
//    			docInstances = getGoldDocInstance(instances, entry, labeled);
//    		}
			for (Instance i : docInstances){
				instances.add(i);
			}
			//break;
    	}
    	System.out.println("Finished making instances");
		return instances;
    }
    
    private static Instances initializeAttributes() {
		String nameOfDataset = "Coref";
		Instances instances = new Instances(nameOfDataset, attributes, 0);
		instances.setClass(classLabel);
		return instances;
    }

    private static Instance create_empty_instance(Instances instances){
		//Magic to create a new instance that will fit with and existing dataset.
		Instance instance = new Instance(instances.numAttributes());
		instance.setDataset(instances);

		// here we can encode the one-hot features
		if(FeatureGenerator.MENTIONFEATURES){
			String attr_name_prefix = "mentionsType1-";
			for (String entityType: ACEAnnotation.getMentionTypes()){
				instance.setValue(attribute_dict.get(attr_name_prefix + entityType), "0");
			}
			attr_name_prefix = "mentionsType2-";
			for (String entityType: ACEAnnotation.getMentionTypes()){
				instance.setValue(attribute_dict.get(attr_name_prefix + entityType), "0");
			}
			attr_name_prefix = "mentionTypePAIR-";
			for (String entityType: ACEAnnotation.getMentionTypes()){
				instance.setValue(attribute_dict.get(attr_name_prefix + entityType), "0");
			}
		}
		if (FeatureGenerator.STRINGFEATURES){
			instance.setValue(attribute_dict.get("extentMatch"),"0");
			instance.setValue(attribute_dict.get("extentSubstring"),"0");
		}
		if (FeatureGenerator.SEMANTICFEATURES){
			
		}
		
		// dont need to do anything for numeric type features
		if(FeatureGenerator.LOCATIONFEATURES){
			
		}
		
		return instance;
    }
    
    /** 
     * This function constructs the Instances for classification.
     * @param instances the set of instances for which the instances will be added to
     * @param entry is the 'document' under which the instances will be constructed
     * @param labeled indicates the training/testing entity
     * @param gold indicates whether we know the gold labels or not.
     */
    private static ArrayList<Instance> getDocInstance(Instances instances, ACEAnnotation entry, Boolean labeled, Boolean gold){
    	ArrayList<Instance> ret = new ArrayList<Instance>();
    	List< CoreferenceEdge > temp = new ArrayList< CoreferenceEdge >();
    	List<String> temp_labels = new ArrayList<String>();
    	
    	if (labeled){
    		// only for training
	    	Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> myLabels = entry.getAllPairsGoldCoreferenceEdges();
	    	// Positive Labels only
	    	int positive_count = myLabels.getFirst().size();
	    	temp.addAll(myLabels.getFirst());
	    	for (int k = 0; k < positive_count; k++){
	    		temp_labels.add("1");
	    	}
	    	
	    	// Sample Negative examples
	    	//System.out.println("Number of positive examples:" + positive_count);
	    	//System.out.println("Number of negative examples:" + myLabels.getSecond().size());
	    	
	    	List<CoreferenceEdge> mylist = myLabels.getSecond();
	    	Collections.shuffle(mylist);
	    	for (int k = 0; k < positive_count & k < mylist.size(); k++){
	    		temp.add(mylist.get(k));
	    		temp_labels.add("-1");
	    	}
	    	//System.out.println("Number of training instances:" + temp.size());
    	}
    	// Testing with gold labels.
		else if (gold){
    		Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> myLabels = entry.getAllPairsGoldCoreferenceEdges();
	    	// Positive Labels only
	    	temp.addAll(myLabels.getFirst());
	    	int positive_count = myLabels.getFirst().size();

	    	for (int k = 0; k < positive_count; k++){
	    		temp_labels.add("1");
	    	}
	    	// Sample Negative examples
	    	//System.out.println("testing number of positive examples:" + positive_count);
	    	List<CoreferenceEdge> mylist = myLabels.getSecond();
	    	Collections.shuffle(mylist);
	    	for (int k = 0; k < positive_count & k < mylist.size(); k++){
	    		temp.add(mylist.get(k));
	    		temp_labels.add("-1");
	    	}
	    	//System.out.println("all testing examples" + temp.size() );
    	} else {
    		temp.addAll( entry.getAllPairsTestCoreferenceEdges() );
    		System.out.println("testing entry with " + temp.size() + " Coreference Edges");
    	}
    	
    	
    	int index = 0;
    	// List<String> document_tokens = entry.getTokens();
    	for (CoreferenceEdge ce : temp ){
    		Instance instance = create_empty_instance(instances);
    		
    		
    		// Adding label to instance
    		if (labeled){
    			// TODO: change to this once isCoreferent is fixed
//	    		if (ce.isCoreferent()){
//					//System.out.println("train label ");
//	    			instance.setClassValue("1");
//				}else{
//					System.out.println("train label negative");
//	    			instance.setClassValue("-1");
//	    		}
    			instance.setClassValue(temp_labels.get(index));
    		}else{
//    			if (ce.isCoreferent()){
//    				//System.out.println("test label positive");
//    				testLabels.add("1");
//    			} else{
//    				System.out.println("test label negative");
//    				testLabels.add("-1");
//    			}
    			testLabels.add(temp_labels.get(index));
    		}
    		
    		
    		instance = setInstanceFeatures(ce, instance);
    		
    		ret.add(instance);
    		index++;
    	}	
    	
		return ret;
    	
    }
    
    public static Instance setInstanceFeatures(CoreferenceEdge ce, Instance instance){
    	Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
		EntityMention e1 = somePair.getFirst();
		EntityMention e2 = somePair.getSecond();
		String key;
		// Adding features to instance
		if(FeatureGenerator.MENTIONFEATURES){
			String attr_name_prefix = "mentionsType1-";
			//System.out.println("features:" + attr_name_prefix + e1.getMentionType());
			instance.setValue(attribute_dict.get(attr_name_prefix + e1.getMentionType()), "1");
			attr_name_prefix = "mentionsType2-";
			instance.setValue(attribute_dict.get(attr_name_prefix + e2.getMentionType()), "1");
			
			attr_name_prefix = "mentionTypePAIR-";
    		String val = e1.getMentionType().compareTo(e2.getMentionType()) == 0  ? "1" : "0"; 
    		
    		if (val.compareTo("0") == 0)
    			instance.setValue(attribute_dict.get(attr_name_prefix + e1.getMentionType()), val);
    		
//    		if (labeled){
//    			if (val.compareTo("0") == 0){
//    				System.out.println("e1: " + e1.getMentionType() + " e2: " + e2.getMentionType() + " equals:" + (e1.getMentionType().compareTo(e2.getMentionType())) );
//	    			
//    				System.out.println("miss-match in training:" + val);
//    			}
//    		}
		}
		
		if (FeatureGenerator.STRINGFEATURES){
			String extent1 = "";
			String extent2 = "";
			for (String word1 : e1.getExtent() ){
				extent1 += word1 + " ";
			}
			for (String word1: e2.getExtent()){
				extent2 += word1 + " ";
			}
			if ( (extent1.trim().toLowerCase()).compareTo(extent2.trim().toLowerCase()) == 0 ){
				System.out.println("extentMatch:" + extent1 + extent2);
				instance.setValue(attribute_dict.get("extentMatch"),"1");
			}
			
			if (extent1.trim().toLowerCase().contains(extent2.trim().toLowerCase()) || extent2.trim().toLowerCase().contains(extent1.trim().toLowerCase()) ){
				System.out.println("extentSubstring:\n" + extent1 + "\n" + extent2);
				instance.setValue(attribute_dict.get("extentSubstring"),"1");
			}
		}
		
		if(FeatureGenerator.LOCATIONFEATURES){
			key = "mentionDistances"; 
			//Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
			instance.setValue(attribute_dict.get(key), distanceFeature(somePair.getFirst(), somePair.getSecond()) );
		}
		return instance;
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
		
		//TODO: fix distance measure
		return Math.abs(e1.getEndOffset() - e2.getStartOffset());
		
		
	}
	
    
    public static void main(String[] args) throws Exception {
		if (args.length != 2) {
		    System.err
			    .println("Usage: FeatureGenerator input-badges-file features-file");
		    System.exit(-1);
		}
		
//		Instances data = readData(args[0]);
//		ArffSaver saver = new ArffSaver();
//		saver.setInstances(data);
//		saver.setFile(new File(args[1]));
//		saver.writeBatch();
    }
}

