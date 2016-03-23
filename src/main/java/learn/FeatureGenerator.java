package learn;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
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
		
		if(MENTIONFEATURES){
			attribute_name = "mentionType";
			a = new Attribute(attribute_name, zeroOne);
			attribute_dict.put(attribute_name, a);
			attributes.addElement(a);
		}
		
		if (STRINGFEATURES){
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
		
		if (SEMANTICFEATURES){
			// need to set before?
			System.setProperty("wordnet.database.dir", "/usr/local/WordNet-3.0/dict/");
			// gender
			// number match
			// wordnet features
			// modifiers match
			// both mentions speak
		}
		
		//generic feature construction
		if(LOCATIONFEATURES){
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
    
    public static Instances readData(ArrayList<ACEAnnotation> data, Boolean labeled){
    	Instances instances = initializeAttributes();
    	for (ACEAnnotation entry : data){
			ArrayList<Instance> Docinstances = getDocInstance(instances, entry, labeled);
			for (Instance i : Docinstances){
				if (i.classIsMissing()){
					System.out.println("testing instance with attribute:" + i.stringValue(attribute_dict.get("mentionType")));
				}
				instances.add(i);
			}
			break;
    	}
    	System.out.println("Finished making instances");
		return instances;
    }
    
//    public static Instances readData(String fileName) throws Exception {
//		Instances instances = initializeAttributes();
//		Scanner scanner = new Scanner(new File(fileName));
//	
//		while (scanner.hasNextLine()) {
//		    String line = scanner.nextLine();
//	
//		    Instance instance = makeInstance(instances, line);
//	
//		    instances.add(instance);
//		}
//		System.out.println("Finished making instances");
//	
//		scanner.close();
//	
//		return instances;
//    }

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
		if(MENTIONFEATURES){
			instance.setValue(attribute_dict.get("mentionType"), "0");
			
		}
		if (STRINGFEATURES){
			instance.setValue(attribute_dict.get("extentMatch"),"0");
			instance.setValue(attribute_dict.get("extentSubstring"),"0");
		}
		if (SEMANTICFEATURES){
			
		}
		
		// dont need to do anything for numeric type features
		if(LOCATIONFEATURES){
			
		}
		
		return instance;
    }
    
    private static ArrayList<Instance> getDocInstance(Instances instances, ACEAnnotation entry, Boolean labeled){
    	ArrayList<Instance> ret = new ArrayList<Instance>();
    	
    	//if (labeled){
    	// only for training
    	Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> myLabels = entry.getAllPairsGoldCoreferenceEdges();
    	// testing
    	//}else{
    	//Pair<List<CoreferenceEdge>, List<CoreferenceEdge>> myLabels = entry.getAllPairsTestCoreferenceEdges();
    	//}
    	
    	
    	// Positive Labels only
    	ArrayList< CoreferenceEdge > temp = new ArrayList< CoreferenceEdge >();
    	//System.out.println("Number of positive examples:" + myLabels.getFirst().size());
    	//System.out.println("Number of negative examples:" + myLabels.getSecond().size());
    	temp.addAll(myLabels.getFirst());
    	temp.addAll(myLabels.getSecond());
    	
    	List<String> document_tokens = entry.getTokens();
    	for (CoreferenceEdge ce : temp ){
    		Instance instance = create_empty_instance(instances);
    		
    		// Adding label to instance
    		if (labeled){
	    		if (ce.isCoreferent()){
	    			instance.setClassValue("1");
				}
	    		else{
	    			instance.setClassValue("-1");
	    		}
    		}else{
    			testLabels.add(ce.isCoreferent() ? "1": "-1");
    		}
    		
    		Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
    		EntityMention e1 = somePair.getFirst();
    		EntityMention e2 = somePair.getSecond();
    		String key;
    		// Adding features to instance
    		if(MENTIONFEATURES){
	    		key = "mentionType";
	    		instance.setValue(attribute_dict.get(key), e1.getMentionType() == e2.getMentionType() ? "1" : "0");
    		}
    		
    		if(LOCATIONFEATURES){
    			key = "mentionDistances"; 
    			//Pair<EntityMention, EntityMention> somePair = ce.getEntityMentions();
    			instance.setValue(attribute_dict.get(key), distanceFeature(somePair.getFirst(), somePair.getSecond()) );
    		}
    		
    		
    		ret.add(instance);
    	}	
    	
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

