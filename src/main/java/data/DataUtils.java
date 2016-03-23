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
 * This class holds utility methods involved in loading the ACE data from the corpus
 *
 * Created by Colin Graber on 3/6/16.
 */
public class DataUtils {

    private static AceFileProcessor processor;

    static {
        //ReadACEAnnotation.is2004mode = false;
        processor = new AceFileProcessor(new CcgTextAnnotationBuilder(new IllinoisTokenizer()));

    }

    public static List<List<ACEAnnotation>> loadDataSplits(String dataDir) {
        List<List<ACEAnnotation>> splits = new ArrayList<List<ACEAnnotation>>();
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

    public static List<ACEAnnotation> loadCorpusFiles(List<File> filePaths) {
        List<ACEAnnotation> result = new ArrayList<ACEAnnotation>();
        for (File file : filePaths) {
            ACEDocument doc = processor.processAceEntry(file.getParentFile(), file.getAbsolutePath());
            result.add(new ACEAnnotation(doc));
        }
        return result;
    }

}

