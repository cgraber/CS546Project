package learn;

import data.ACEAnnotation;

import java.util.List;

/**
 * This defines an interface that should be implemented by each stage of the pipeline.
 *
 * Created by Colin Graber on 3/21/16.
 */
public interface PipelineStage {

    /**
     * This method should train the model for whatever stage the implementing class handles
     * @param data The training data
     */
    void trainModel(List<ACEAnnotation> data);

    /**
     * This method should add the appropriate test labels for whatever stage the implementing class handles
     * @param data
     */
    void test(List<ACEAnnotation> data);
}
