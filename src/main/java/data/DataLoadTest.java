package data;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.nlp.tokenizer.IllinoisTokenizer;
import edu.illinois.cs.cogcomp.nlp.utility.CcgTextAnnotationBuilder;
import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.*;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.AceFileProcessor;
import edu.illinois.cs.cogcomp.reader.ace2005.documentReader.ReadACEAnnotation;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Colin Graber on 2/27/16.
 */
public class DataLoadTest {
    public static final String RELATION_VIEW = "RELATION";

    public static void main(String [] argv) {
        //ReadACEAnnotation.is2004mode = false;
        AceFileProcessor processor = new AceFileProcessor(new CcgTextAnnotationBuilder(new IllinoisTokenizer()));

        String aceCorpusDir = "/home/cgraber/Documents/Illinois/Classes/CS546/project/ACE05_English";

        //String aceCorpusDir = "/home/cgraber/Documents/Illinois/Classes/CS546/project/ACE04/data/English";

        File inputFolder = new File (aceCorpusDir);
        File[] subFolderList = inputFolder.listFiles();

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File directory, String fileName) {
                return fileName.endsWith(".apf.xml");
            }
        };
        int fileCount = 0;
        int multilineCount = 0;
        int probs = 0;
        int relCount = 0;
        int totalRels = 0;
        Map<String, Integer> countPerFolder = new HashMap<String, Integer>();
        Map<String, Integer> multCountPerFolder = new HashMap<String, Integer>();
        for (int folderIndex = 0; folderIndex < subFolderList.length; ++folderIndex) {
            File subFolderEntry = subFolderList[folderIndex];
            countPerFolder.put(subFolderEntry.getName(), 0);
            multCountPerFolder.put(subFolderEntry.getName(), 0);

            if (!subFolderEntry.getName().equals("bn") && !subFolderEntry.getName().equals("nw")) {
                continue;
            }

            File[] fileList = subFolderEntry.listFiles(filter);
            if (fileList == null) {
                continue;
            }
            for (int fileID = 0; fileID < fileList.length; ++fileID) {
                fileCount++;
                String annotationFile = fileList[fileID].getAbsolutePath();
                //System.out.println(annotationFile);
                ACEDocument doc;
                try {
                    doc = processor.processAceEntry(subFolderEntry, annotationFile);
                } catch (Exception e) {
                    probs++;
                    continue;
                }
                List<TextAnnotation> taList = AceFileProcessor.populateTextAnnotation(doc);

                countPerFolder.put(subFolderEntry.getName(), countPerFolder.get(subFolderEntry.getName())+1);
                if (taList.size() == 1) {
                    multilineCount++;
                    System.out.println(annotationFile);
                    multCountPerFolder.put(subFolderEntry.getName(), multCountPerFolder.get(subFolderEntry.getName())+1);
                    for (TextAnnotation ta: taList) {
                        System.out.println("\t"+ta.getText());
                    }
                }

                // Add relations to text annotations
                /*
                for (ACEEntity entity: doc.aceAnnotation.entityList) {
                    //System.out.println("Entity ID: "+entity.id);
                    //System.out.println("\tclassEntity: "+entity.classEntity);
                    //System.out.println("\ttype: "+entity.type);
                    //System.out.println("\tsubtype: "+entity.subtype);
                    //System.out.println("\tMentions:");
                    for (ACEEntityMention mention: entity.entityMentionList) {
                        //System.out.println("\t\tid = "+mention.id + ", type = "+mention.type + ", extent = ("+mention.extentStart+", "+mention.extentEnd+"): "+mention.extent);
                    }
                }*/
                for (ACERelation relation: doc.aceAnnotation.relationList) {
                    System.out.println("Relation ID: "+relation.id);
                    System.out.println("\ttype: "+relation.type);
                    System.out.println("\tsubtype: "+relation.subtype);
                    System.out.println("\tmodality: "+relation.modality);
                    System.out.println("\ttense: "+relation.tense);
                    System.out.println("\trelation Args:");
                    for (ACERelationArgument arg: relation.relationArgumentList) {
                        System.out.println("\t\tid = "+arg.id+", role = "+arg.role);
                    }
                    System.out.println("\trelation mentions:");
                    totalRels++;
                    if (relation.relationMentionList.size() > 1) {
                        relCount++;
                    }
                    for (ACERelationMention mention: relation.relationMentionList) {
                        System.out.println("\t\tid = "+mention.id + ", lexicalCondition = "+mention.lexicalCondition + ", extent = ("+mention.extentStart+", "+mention.extentEnd+")");
                        for (ACERelationArgumentMention argMention: mention.relationArgumentMentionList) {
                            System.out.println("\t\t\tid = "+argMention.id + ", role = "+argMention.role+", extent = ("+argMention.start+", "+argMention.end+")");
                        }
                    }
                }
            }
        }
        System.out.println("NUMBER OF FILES: "+fileCount);
        System.out.println("MULTILINE FILES: "+multilineCount);
        for (String fname: countPerFolder.keySet()) {
            System.out.println("\tFolder: "+fname+", count = "+multCountPerFolder.get(fname)+", total = "+countPerFolder.get(fname));
        }
        System.out.println("FAILED: "+probs);
        System.out.println("NUMBER MULT. RELATIONS: "+relCount);
        System.out.println("TOTAL REL NO: "+totalRels);
    }
}
