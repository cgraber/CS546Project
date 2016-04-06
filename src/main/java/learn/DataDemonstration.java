package learn;

import data.ACEAnnotation;
import data.CoreferenceEdge;
import data.EntityMention;
import data.Relation;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import utils.Consts;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Colin Graber on 3/21/16.
 */
public class DataDemonstration {

    /*
     * Don't run this code - it's not meant to be used to do anything. Rather, it's a demonstration of some of the
     * things you can do with the data
     */
    public void demonstration(List<ACEAnnotation> data) {
        //An ACEAnnotation represents all of the data contained within a single document. It contains (or should contain)
        //all of the methods you need to access this data for whatever purposes you need it for. I'll demonstrate:

        //First, note that the methods in the PipelineStage interface take in a list of ACEAnnotations - the actual
        //documents contained within this list will be controlled by whatever class is used to glue all of the stages
        //together - so no need to worry about that detail!

        for (ACEAnnotation document: data) {
            //The simplest thing you'll want from the document is the actual words and sentences themselves. Here are
            //the methods related to that.

            //First, one of the simplest things - if for some reason you need the number of sentences, here you are!
            document.getNumberOfSentences();

            //There are two ways one can view a document. The first is as a collection of sentences. This method
            //gives you all of the tokens in the document, arranged by sentences.
            List<List<String>> allSentences = document.getSentences();
            //Given this, you can loop through the sentences as follows:
            for (List<String> sentence: allSentences) {
                //And then every word in the sentence as follows:
                for (String word: sentence) {
                    //DO SOMETHING
                }
            }

            //In case you only need to access one sentence, there's a simple way of doing this too:
            document.getSentence(0);

            //The second way you can view a document is as a collection of tokens - this view is important if you don't
            //really care about sentences (EntityMention, for example, indexes words based on this point of view)
            List<String> allTokens = document.getTokens();

            //You can also access one token:
            document.getToken(0);


            //For the time being, you can access two different kinds of annotations for each of the tokens in the document

            //First, POS tags. If you're using the sentence view, you'll want to use the following:
            List<List<String>> posTagsBySentence = document.getPOSTagsBySentence();

            //And if you're just working with tokens, you'll want to use the following:
            List<String> posTags = document.getPOSTags();

            //The other annotation you can get is Lemmas. This follows the same pattern as POS tags:
            List<List<String>> lemmasBySentence = document.getLemmasBySentence();
            List<String> lemmas = document.getLemmas();


            //Now we come to the topic of how the gold/test data is represented. We'll go through each of the tasks
            //in order

            //The main class holding NER data is EntityMention. This class is very important - it's used for the
            //other tasks as well (since both Relation Extraction and Coreference Resolution work on pairs of entity mentions)

            //Since we're treating NER as a sequence labeling task, the training code will need the gold sequence labels.
            //This methods gives them to you (note that this is in "sentence form", since sequence labeling tasks tend
            //to process one sentence at a time
            List<List<String>> goldBIOTags = document.getGoldBIOEncoding();

            //You'll probably also want to have access to all of the possible sequence labels - here they are!
            Set<String> possibleLabels = ACEAnnotation.getBIOLabels();

            //Here's a key thing: at test time, the NER system needs to be able to record the entities it finds.
            //The general paradigm for this code is that new labels are added to the documents you are passed in, rather
            //than constructing something new and returning it. Hence, for each task, there is a method that allows you
            //to add a label. For NER, it looks like this:
            //                        MENTION TYPE    START INDEX   END INDEX+1
            document.addEntityMention("[TYPE]",            0,           2,           0,            2);
            //NOTE: there are two important facts about the span arguments:
            //      1) These indices are global, not per-sentence. So, for example, if you are adding a mention in the second
            //         sentence, you need to make sure that the indices you pass in here are not the indices within that sentence
            //         but rather are the indices from the start of the document
            //      2) The last argument is END INDEX+1 - for example, if the first two words in your document are
            //         "President Obama", the start index is 0 and the end index is 2 (this is a consequence of how
            //         these annotations will end up being represented once the kinks are worked out of the Illinois NLP
            //         library)

            //Now we move to the other two annotation tasks, Relation Extraction and Coreference Resolution.
            //Both of these look somewhat similar from a data standpoint due to the way we're modeling these tasks
            //for now, so I'll be handling them both together.

            //First things first: to do training/testing, you will need the EntityMentions in the document.
            //At training time, get the gold Entities using this method:
            List<EntityMention> goldMentions = document.getGoldEntityMentions();
            //At test time, get the EntityMentions annotated by the NER component using this method:
            List<EntityMention> testMentions = document.getTestEntityMentions();

            //Before I move on, quick introduction to the classes representing Relations and Coreference Edges.

            //The Relation class consists of three pieces of information: the EntityMention that comprises Arg1, the
            //EntityMention that comprises Arg2, and the relation between them. These can be accessed via:
            Relation sampleRelation = document.getGoldRelations().get(0);
            String sampleRelationType = sampleRelation.getType();
            EntityMention sampleRelationArg1 = sampleRelation.getArg1();
            EntityMention sampleRelationArg2 = sampleRelation.getArg2();
            //The full space of labels can be accessed via:
            Set<String> relationLabels = ACEAnnotation.getRelationTypes();
            //There is one special label representing no relation between the entities - this can be found here:
            String noRelLabel = Consts.NO_REL;

            //The CoreferenceEdge also has three pieces of information: the two EntityMentions in question, and a boolean
            //value representing whether they are or are not coreferent:
            CoreferenceEdge sampleEdge = document.getGoldCoreferenceEdges().get(0);
            Pair<EntityMention,EntityMention> sampleEdgeEntities = sampleEdge.getEntityMentions();
            EntityMention sampleEdgeE1 = sampleEdgeEntities.getFirst();
            EntityMention sampleEdgeE2 = sampleEdgeEntities.getSecond();
            boolean sampleIsCoreferent = sampleEdge.isCoreferent();

            //There are two ways to get the gold data for each of these.
            //First, if you only want all of the explicit relations/edges (i.e., for relation extraction, every relation
            //that is not "no relation", and for coreference all of the "true" edges), use one of these (based on
            //what works best for you):
            List<CoreferenceEdge> goldEdges = document.getGoldCoreferenceEdges();
            Map<Pair<EntityMention,EntityMention>,CoreferenceEdge> goldEdgesByEntities = document.getGoldCoreferenceEdgesByEntities();
            List<Relation> goldRelations = document.getGoldRelations();
            Map<Pair<EntityMention,EntityMention>,Relation> goldRelationsByArgs = document.getGoldRelationsByArgs();

            //The second way allows you to get relations/edges for "all pairs" of entities - this will include both
            //"Positive edges" and "Negative edges" (i.e. "no relation" relations or "false" coreference edges)
            Pair<List<CoreferenceEdge>,List<CoreferenceEdge>> allPairsGoldEdges = document.getAllPairsGoldCoreferenceEdges();
            Pair<List<Relation>,List<Relation>> allPairsGoldRelations = document.getAllPairsGoldRelations();
            //In both of these, the first entry in the pair is the list of "Positive" edges, and the second entry is
            //The list of "negative" edges.

            //At test time, to add a label, use one of the following methods:
            document.addRelation("[TYPE]", sampleEdgeE1, sampleEdgeE2);
            document.addCoreferenceEdge(sampleEdgeE1, sampleEdgeE2);


            //As for how to get features...because each of the three systems are potentially using separate libraries,
            //feature representations may differ. So you'll need to figure out what works best for you and run with it

            //I've added one simple way of representing features, in case all you have to pass in is a binary feature
            //vector and nothing else. This resides in the class FeatureVector, which keeps track of indices for you.

            //The way it works is that it globally keeps track of the indices of the feature vector as a map between
            //a string (which is the name of the feature) and its index in the vector. The way to use this is to create
            //a constant String (preferably within the Consts class) for each feature type, and append any necessary
            //parameters. For example:
            FeatureVector sampleVec = new FeatureVector();
            sampleVec.addBinaryFeature(Consts.POS_FEATURE + posTags.get(0));
            sampleVec.addBinaryFeature(Consts.POS_FEATURE + posTags.get(0) + "_" + allTokens.get(0));

            //And then, to get your vector:
            List<Integer> features = sampleVec.getFeatures();

            //One thing worth noting is that, as you add features, the size of the feature vector will grow. So you have
            //to make sure that however your weights are represented that it can handle this (one could write a very similar
            //class to this one to handle this)

            //Of course, you don't have to use this - feel free to do whatever works best for you!


        }
    }
}
