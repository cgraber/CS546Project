package utils;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.Configurator;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.nlp.pipeline.IllinoisPipelineFactory;

import java.util.Properties;

/**
 * Created by Colin Graber on 3/13/16.
 */
public class Pipeline {

    private static AnnotatorService pipeline;

    static {
        pipeline = buildPipeline();
    }

    public static AnnotatorService buildPipeline() {
        Properties props = new Properties();
        props.setProperty(PipelineConfigurator.USE_POS.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_LEMMA.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_SHALLOW_PARSE.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_NER_CONLL.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_NER_ONTONOTES.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_STANFORD_PARSE.key, Configurator.TRUE);
        props.setProperty(PipelineConfigurator.USE_STANFORD_DEP.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_SRL_VERB.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.USE_SRL_NOM.key, Configurator.FALSE);
        props.setProperty(PipelineConfigurator.STFRD_MAX_SENTENCE_LENGTH.key, "10000");
        props.setProperty(PipelineConfigurator.STFRD_TIME_PER_SENTENCE.key, "100000000");

        ResourceManager rm = new ResourceManager(props);
        AnnotatorService pipeline = null;
        try {
            pipeline = IllinoisPipelineFactory.buildPipeline(rm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pipeline;
    }

    public static void addAllViews(TextAnnotation ta) {
        try {
            pipeline.addView(ta, ViewNames.POS);
            pipeline.addView(ta, ViewNames.LEMMA);
            //pipeline.addView(ta, ViewNames.PARSE_STANFORD);
        } catch (AnnotatorException e) {
            System.err.println("PIPELINE PROBLEM - THIS SHOULDN'T HAPPEN");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
