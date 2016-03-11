package data;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.*;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.ReadACEAnnotation;
import edu.illinois.cs.cogcomp.reader.util.EventConstants;
import org.apache.commons.lang.ArrayUtils;
import utils.Consts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.*;


/**
 * Created by Colin Graber on 3/6/16.
 */
public class DataUtils {
    private static AceFileProcessor processor;
    private static Set<String> relationTypes;
    private static Set<String> relationSubtypes;

    static {
        ReadACEAnnotation.is2004mode = false;
        processor = new AceFileProcessor(new CcgTextAnnotationBuilder(new IllinoisTokenizer()));
        relationTypes = new HashSet<String>();
        relationSubtypes = new HashSet<String>();
    }

    public static Set<String> getRelationTypes() {
        return relationTypes;
    }

    public static Set<String> getRelationSubtypes() {
        return relationSubtypes;
    }

    public static List<List<TextAnnotation>> loadDataSplits(String dataDir) {
        List<List<TextAnnotation>> splits = new ArrayList<List<TextAnnotation>>();
        for (int i = 0; i < 5; i++) {
            File splitFile = new File("config/splits/split"+i+".txt");
            try (BufferedReader br = new BufferedReader(new FileReader(splitFile))) {
                String line;
                List<File> paths = new ArrayList<File>();
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith("//")) {
                        continue;
                    }

                    paths.add(new File(dataDir, line));
                }
                splits.add(loadCorpusFiles(paths));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        return splits;
    }

    public static List<TextAnnotation> loadCorpusFiles(List<File> filePaths) {
        List<TextAnnotation> result = new ArrayList<TextAnnotation>();
        for (File file : filePaths) {
            System.out.println(file);
            ACEDocument doc = processor.processAceEntry(file.getParentFile(), file.getAbsolutePath());
            if (doc.taList.size() > 1) {
                continue; // TODO: make work for more than one text annotation in doc
            }
            result.add(makeTA(doc));
        }
        return result;
    }

    public static TextAnnotation makeTA(ACEDocument doc) {
        //TODO: make work for more than one text annotation in doc
        TextAnnotation ta = doc.taList.get(0).getTa();

        //Initialize coreference information
        CoreferenceView corefView = new CoreferenceView(Consts.VIEW_COREF_GOLD, ta);
        ta.addView(Consts.VIEW_COREF_GOLD, corefView);
        for (ACEEntity entity : doc.aceAnnotation.entityList) {
            Constituent canonical = null;
            List<Constituent> mentions = new ArrayList<Constituent>();
            for (ACEEntityMention mention : entity.entityMentionList) {
                if (canonical == null) { //TODO: is first mention in list actually canonical?
                    canonical = makeCorefConstituent(mention, ta);
                } else {
                    mentions.add(makeCorefConstituent(mention, ta));
                }
            }
            double [] scores = new double[mentions.size()];
            corefView.addCorefEdges(canonical, mentions, scores);
        }

        //Initialize relation information
        PredicateArgumentView relationView = new PredicateArgumentView(Consts.VIEW_RELATION_GOLD, ta);
        ta.addView(Consts.VIEW_RELATION_GOLD, relationView);
        for (ACERelation relation : doc.aceAnnotation.relationList) {
            for (ACERelationMention relMention: relation.relationMentionList) {
                Constituent pred = makeRelationPredConstituent(relation, relMention, ta);
                Pair<List<Constituent>, List<String>> args = makeRelationArgConstituents(relMention, ta);
                String [] roleArr = new String[args.getSecond().size()];
                double [] scores = new double[args.getSecond().size()];
                relationView.addPredicateArguments(pred, args.getFirst(), args.getSecond().toArray(roleArr), scores);
            }
        }

        return ta;
    }

    private static Pair<List<Constituent>, List<String>> makeRelationArgConstituents(ACERelationMention relMention, TextAnnotation ta) {
        List<Constituent> result = new ArrayList<Constituent>();
        List<String> roles = new ArrayList<String>();
        for (ACERelationArgumentMention argMention: relMention.relationArgumentMentionList) {
            IntPair offsets = findTokenOffsets(ta, argMention.start, argMention.end);
            Constituent arg = new Constituent(argMention.id, 1.0, Consts.VIEW_RELATION_GOLD, ta, offsets.getFirst(), offsets.getSecond());
            result.add(arg);
            roles.add(argMention.role);
        }
        return new Pair<>(result, roles);
    }

    private static Constituent makeRelationPredConstituent(ACERelation relation, ACERelationMention relMention, TextAnnotation ta) {
        IntPair offsets = findTokenOffsets(ta, relMention.extentStart, relMention.extentEnd);
        Constituent constituent = new Constituent(relMention.id, 1.0, Consts.VIEW_RELATION_GOLD, ta, offsets.getFirst(), offsets.getSecond());
        constituent.addAttribute(Consts.ATTR_REL_TYPE, relation.type);
        constituent.addAttribute(Consts.ATTR_REL_SUBTYPE, relation.subtype);

        // Record types seen
        relationTypes.add(relation.type);
        relationTypes.add(relation.subtype);

        return constituent;
    }

    private static Constituent makeCorefConstituent(ACEEntityMention mention, TextAnnotation ta) {
        //TODO: is label important?
        IntPair offsets = findTokenOffsets(ta, mention.extentStart, mention.extentEnd);
        System.out.println(ta.getText());
        System.out.println(mention.extent);
        System.out.println(Arrays.asList(ta.getTokens()));
        Constituent constituent = new Constituent(mention.id, 1.0, Consts.VIEW_COREF_GOLD, ta, offsets.getFirst(), offsets.getSecond());
        constituent.addAttribute(Consts.ATTR_COREF_TYPE, mention.type);
        constituent.addAttribute(Consts.ATTR_COREF_LDCTYPE, mention.ldcType);
        constituent.addAttribute(Consts.ATTR_COREF_HEAD_END, Integer.toString(mention.headStart));
        constituent.addAttribute(Consts.ATTR_COREF_HEAD_END, Integer.toString(mention.headEnd));

        return constituent;
    }

    /**
     * find the corresponding token id from the TextAnnotation ta for the char offset given.
     * Necessary because source text is split into multiple TextAnnotation objects, but char offsets
     *    are not natively preserved, but written into additional view.
     * @param ta    TextAnnotation to search for mention
     * @param mentionStart mention start offset
     * @param mentionEnd    mention end offset
     * @return  token offsets of corresponding Constituent from the TextAnnotation, or null if not found
     */
    private static IntPair findTokenOffsets(TextAnnotation ta, int mentionStart, int mentionEnd )
    {
        int tokenStart = -1;
        int tokenEnd = -1;

        IntPair tokenOffsets = null;
        View tokenOffsetView = ta.getView( EventConstants.TOKEN_WITH_CHAR_OFFSET );


        for ( Constituent t : tokenOffsetView.getConstituents() )
        {
            if ( Integer.parseInt( t.getAttribute(EventConstants.CHAR_START ) ) == mentionStart )
                tokenStart = t.getStartSpan();

            if ( Integer.parseInt(t.getAttribute(EventConstants.CHAR_END) ) == mentionEnd )
                tokenEnd = t.getEndSpan();
        }

        if ( tokenStart >= 0 && tokenEnd >= 0 )
        {
            tokenOffsets = new IntPair( tokenStart, tokenEnd );
        }

        return tokenOffsets;
    }
}

