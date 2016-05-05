package data;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.*;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.aceReader.annotationStructure.ACEDocument;
import edu.illinois.cs.cogcomp.nlp.corpusreaders.aceReader.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.TokenizerTextAnnotationBuilder;
import org.apache.commons.lang.ArrayUtils;
import utils.Consts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.*;


/**
 * This class holds utility methods involved in loading the ACE data from the corpus
 *
 * Created by Colin Graber on 3/6/16.
 */
public class DataUtils {

    private static AceFileProcessor processor;

    static {
        //ReadACEAnnotation.is2004mode = false;
        processor = new AceFileProcessor(new TokenizerTextAnnotationBuilder(new IllinoisTokenizer()));

    }

    public static List<List<ACEAnnotation>> loadDataSplits(String dataDir) {
        List<List<ACEAnnotation>> splits = new ArrayList<List<ACEAnnotation>>();
        File trainFile = new File("config/splits/training_files.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(trainFile))) {
            String line;
            List<File> paths = new ArrayList<File>();
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("//")) {
                    continue;
                }

                paths.add(new File(dataDir, line));
            }
            splits.add(loadCorpusFiles(paths));
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        File testFile = new File("config/splits/test_files.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(testFile))) {
            String line;
            List<File> paths = new ArrayList<File>();
            while ((line = br.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("//")) {
                    continue;
                }

                paths.add(new File(dataDir, line));
            }
            splits.add(loadCorpusFiles(paths));
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }


        return splits;
    }

    public static List<ACEAnnotation> loadCorpusFiles(List<File> filePaths) {
        List<ACEAnnotation> result = new ArrayList<ACEAnnotation>();
        try {
            for (File file : filePaths) {
                ACEDocument doc = processor.processAceEntry(file.getParentFile(), file.getAbsolutePath());
                result.add(new ACEAnnotation(doc));
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return result;
    }

}

