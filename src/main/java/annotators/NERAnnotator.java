package annotators;

import data.ACEAnnotation;
import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.ACEReader;
import experiments.NERBaseline;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin Graber on 4/16/16.
 */
public class NERAnnotator extends Annotator {

    public NERAnnotator() {
        super(ACEReader.ENTITYVIEW, new String[] {ViewNames.POS, ViewNames.LEMMA});
    }

    @Override
    public void addView(TextAnnotation ta) throws AnnotatorException {
        ACEAnnotation doc = new ACEAnnotation(ta);
        NERBaseline ner = new NERBaseline();
        List<ACEAnnotation> arg = new ArrayList<>();
        arg.add(doc);
        ner.test(arg);
    }
}
