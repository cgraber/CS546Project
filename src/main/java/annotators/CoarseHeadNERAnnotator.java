package annotators;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.openeval.learner.Server;
import edu.illinois.cs.cogcomp.openeval.learner.ServerPreferences;

import java.io.IOException;

/**
 * Created by Colin Graber on 5/10/16.
 */
public class CoarseHeadNERAnnotator extends NERAnnotator {
    public CoarseHeadNERAnnotator() {
        super(true, ViewNames.NER_ACE_COARSE_HEAD);
    }

    public static void main(String[] argv) throws IOException {
        Annotator annotator = new CoarseHeadNERAnnotator();

        //The second number in the ServerPreferences constructor is the number of TextAnnotations sent at a time to
        //be annotated. Because of the slow speed of NER labeling, this was set to 1; setting it to 50 (as listed in
        //the documentation) caused the connection to time out.
        Server server = new Server(5757, new ServerPreferences(0, 50), annotator);

        fi.iki.elonen.util.ServerRunner.executeInstance(server);
    }
}
