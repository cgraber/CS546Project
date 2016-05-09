package annotators;
import data.ACEAnnotation;
import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ACEReader;
import edu.illinois.cs.cogcomp.openeval.learner.Server;
import edu.illinois.cs.cogcomp.openeval.learner.ServerPreferences;
import experiments.CorefBaseline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CorefBaselineAnnotator extends Annotator {
	
	/**
	 * Created by Alex Morales on 4/19/16.
	 */
	private CorefBaseline cb = new CorefBaseline();
	
    public CorefBaselineAnnotator() {
    	super(ViewNames.COREF, new String[] {ViewNames.POS, ViewNames.LEMMA, ViewNames.PARSE_STANFORD});
    	 // loading processed data
        List<List<ACEAnnotation>> splits = null;
        try {
            splits = ACEAnnotation.readAllFromFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // temporary training
		ArrayList<ACEAnnotation> train_split = new ArrayList<ACEAnnotation>();
		for (int i = 0; i < splits.size()-1; i++){
			train_split.addAll( splits.get(i));
		}
		this.cb.trainModel(train_split);   
    }
    
    @Override
    public void addView(TextAnnotation ta) throws AnnotatorException {
        //When writing an annotator for our system, there are two main steps:
        //First, you need to make an ACEAnnotation from the text annotation. There's a constructor to do this:
        ACEAnnotation doc = new ACEAnnotation(ta);
        
        // All you have to do now is run your test code on this ACEAnnotation
        List<ACEAnnotation> arg = new ArrayList<>();
        
        // can predict and add is coreferent but it will it make sense ??
        arg.add(doc);
        this.cb.test(arg);
    }
    
    public static void main(String[] argv) throws IOException {
        Annotator annotator = new CorefBaselineAnnotator();
        //The second number in the ServerPreferences constructor is the number of TextAnnotations sent at a time to
        //be annotated. Because of the slow speed of NER labeling, this was set to 1; setting it to 50 (as listed in
        //the documentation) caused the connection to time out.
        Server server = new Server(5757, new ServerPreferences(0, 1), annotator);
        fi.iki.elonen.util.ServerRunner.executeInstance(server);
    }
    
    
}
