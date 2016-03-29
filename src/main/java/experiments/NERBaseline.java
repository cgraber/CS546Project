package experiments;

import data.ACEAnnotation;
import learn.PipelineStage;

import java.util.List;

/**
 * Created by Colin Graber on 3/29/16.
 */
public class NERBaseline implements PipelineStage {

    public void trainModel(List<ACEAnnotation> data) {
        buildFeatureFile(data);
        runMallet();
    }

    public void test(List<ACEAnnotation> data) {

    }

    private void buildFeatureFile(List<ACEAnnotation> data) {
        for (ACEAnnotation doc: data) {
            List<List<String>> bioLabels = doc.getGoldBIOEncoding();
            for (int i = 0; i < doc.getNumberOfSentences(); i++) {
                List<String> features = extractFeatures(doc.getSentence(i));
            }

        }
    }

    private List<String> extractFeatures(List<String> sentence) {

    }
}
