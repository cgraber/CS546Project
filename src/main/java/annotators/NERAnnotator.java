package annotators;

import data.ACEAnnotation;
import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ACEReader;
import edu.illinois.cs.cogcomp.openeval.learner.ServerPreferences;
import experiments.NERBaseline;
import edu.illinois.cs.cogcomp.openeval.learner.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin Graber on 4/16/16.
 */
public class NERAnnotator extends Annotator {

    private boolean isCoarse;

    public NERAnnotator(boolean isCoarse, String viewName) {
        super(viewName, new String[] {ViewNames.POS, ViewNames.LEMMA, ViewNames.PARSE_STANFORD});
        this.isCoarse = isCoarse;
    }

    @Override
    public void addView(TextAnnotation ta) throws AnnotatorException {
        System.out.println("Starting with doc!");

        //When writing an annotator for our system, there are two main steps:
        //First, you need to make an ACEAnnotation from the text annotation. There's a constructor to do this:
        ACEAnnotation doc = new ACEAnnotation(ta);

        //All you have to do now is run your test code on this ACEAnnotation
        NERBaseline ner = new NERBaseline(isCoarse);
        List<ACEAnnotation> arg = new ArrayList<>();
        arg.add(doc);
        ner.test(arg);
        System.out.println("Done with doc!");
        //As long as your code adds the NER, coreference, or relation information to the ACEAnnotation document, the
        //correct information will be automatically added to the TextAnnotation!
    }
}
