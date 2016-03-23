package data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class EntityMention implements Serializable {
    private String entityType;
    private int startOffset;
    private int endOffset;
    private int sentenceOffset;
    private ACEAnnotation annotation;
    private String mentionType;

    protected EntityMention(String entityType, String mentionType, int startOffset, int endOffset, int sentenceOffset, ACEAnnotation annotation) {
        this.entityType = entityType;
        this.mentionType = mentionType;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.sentenceOffset = sentenceOffset;
        this.annotation = annotation;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getMentionType() {
        return mentionType;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getSentenceOffset() {return sentenceOffset; }

    //NOTE THAT THIS RETURNS THE INDEX OF THE END TOKEN + 1
    public int getEndOffset() {
        return endOffset;
    }

    public List<String> getExtent() {
        return annotation.getExtent(startOffset, endOffset);
    }

    public boolean equals(EntityMention other) {
        if (this.entityType.equals(other.entityType) &&
                this.startOffset == other.startOffset &&
                this.endOffset == other.endOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }
}
