package data;

import edu.illinois.cs.cogcomp.reader.ace2005.annotationStructure.ACEEntityMention;

import java.util.List;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class EntityMention {
    private String type;
    private int startOffset;
    private int endOffset;
    private ACEAnnotation annotation;

    public EntityMention(String type, int startOffset, int endOffset, ACEAnnotation annotation) {
        this.type = type;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.annotation = annotation;
    }

    public String getType() {
        return type;
    }

    public int getStartOffset() {
        return startOffset;
    }

    //NOTE THAT THIS RETURNS THE INDEX OF THE END TOKEN + 1
    public int getEndOffset() {
        return endOffset;
    }

    public List<String> getExtent() {
        return annotation.getExtent(startOffset, endOffset);
    }

    public boolean equals(EntityMention other) {
        if (this.type.equals(other.type) &&
                this.startOffset == other.startOffset &&
                this.endOffset == other.endOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }
}
