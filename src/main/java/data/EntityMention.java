package data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class EntityMention implements Serializable {

    private static final long serialVersionUID = 3L;

    public String entityType;
    public String mentionType;

    public int extentStartOffset;
    public int extentEndOffset;
    public int headStartOffset;
    public int headEndOffset;
    public int sentenceOffset;

    public ACEAnnotation annotation;
    public GISentence sentence;
    public int corefGroupIndex;

    protected EntityMention(String entityType, String mentionType, int extentStartOffset, int extentEndOffset, int headStartOffset, int headEndOffset, int sentenceOffset, ACEAnnotation annotation) {
        this.entityType = entityType;
        this.mentionType = mentionType;
        this.extentStartOffset = extentStartOffset;
        this.extentEndOffset = extentEndOffset;
        this.headStartOffset = headStartOffset;
        this.headEndOffset = headEndOffset;
        this.sentenceOffset = sentenceOffset;
        this.annotation = annotation;
        this.corefGroupIndex=-1;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getMentionType() {
        return mentionType;
    }

    public int getExtentStartOffset() {
        return extentStartOffset;
    }

    public int getHeadStartOffset() {
        return headStartOffset;
    }

    public int getHeadEndOffset() {
        return headEndOffset;
    }

    public int getSentenceOffset() { return sentenceOffset; }

    //NOTE THAT THIS RETURNS THE INDEX OF THE END TOKEN + 1
    public int getExtentEndOffset() {
        return extentEndOffset;
    }

    public List<String> getExtent() {
        return annotation.getExtent(extentStartOffset, extentEndOffset);
    }

    public List<String> getHead() {
        return annotation.getExtent(headStartOffset, headEndOffset);
    }

    public boolean equals(EntityMention other) {
        if (this.entityType.equals(other.entityType) &&
                this.extentStartOffset == other.extentStartOffset &&
                this.extentEndOffset == other.extentEndOffset &&
                this.headStartOffset == other.headStartOffset &&
                this.headEndOffset == other.headEndOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }

    public boolean equalsHead(EntityMention other) {
        if (this.entityType.equals(other.entityType) &&
                this.headStartOffset == other.headStartOffset &&
                this.headEndOffset == other.headEndOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }
}
