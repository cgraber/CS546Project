package data;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class EntityMention implements Serializable, Comparable<EntityMention> {


    private static final long serialVersionUID = 3L;


    public String coarseEntityType;
    public String fineEntityType;
    public String mentionType;

    public int extentStartOffset;
    public int extentEndOffset;
    public int headStartOffset;
    public int headEndOffset;
    public int sentenceOffset;

    public ACEAnnotation annotation;
    public GISentence sentence;
    private Constituent constituent = null;
    public int corefGroupIndex;


    protected EntityMention(String coarseEntityType, String fineEntityType, String mentionType, int extentStartOffset, int extentEndOffset, int headStartOffset, int headEndOffset, int sentenceOffset, ACEAnnotation annotation) {
        this.coarseEntityType = coarseEntityType;
        this.fineEntityType = fineEntityType;
        this.mentionType = mentionType;
        this.extentStartOffset = extentStartOffset;
        this.extentEndOffset = extentEndOffset;
        this.headStartOffset = headStartOffset;
        this.headEndOffset = headEndOffset;
        this.sentenceOffset = sentenceOffset;
        this.annotation = annotation;
        this.corefGroupIndex = -1;
    }

    public String getCoarseEntityType() {
        return coarseEntityType;
    }

    public String getFineEntityType() {
        return fineEntityType;
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

    public int getSentenceOffset() {
        return sentenceOffset;
    }

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

    public boolean equalsCoarseExtent(EntityMention other) {
        if (this.coarseEntityType.equals(other.coarseEntityType) &&
                this.extentStartOffset == other.extentStartOffset &&
                this.extentEndOffset == other.extentEndOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }

    public boolean equalsFineExtent(EntityMention other) {
        if (this.fineEntityType.equals(other.fineEntityType) &&
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


    public boolean equalsCoarseHead(EntityMention other) {
        if (this.coarseEntityType.equals(other.coarseEntityType) &&
                this.headStartOffset == other.headStartOffset &&
                this.headEndOffset == other.headEndOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }

    public boolean equalsFineHead(EntityMention other) {
        if (this.fineEntityType.equals(other.fineEntityType) &&
                this.headStartOffset == other.headStartOffset &&
                this.headEndOffset == other.headEndOffset &&
                this.annotation == other.annotation) {
            return true;
        } else {
            return false;
        }
    }

    public void setConstituent(Constituent constituent) {
        this.constituent = constituent;
    }

    public Constituent getConstituent() {
        return constituent;
    }

    @Override
    public int compareTo(EntityMention o) {
        if (equalsFineExtent(o)) {
            return 0;
        }
        if ((this.extentStartOffset < o.extentStartOffset) || (this.headStartOffset < o.headStartOffset)
                || (this.extentEndOffset < o.extentEndOffset) || (this.headEndOffset < o.headEndOffset)) {
            return -1;
        }
        return 1;
    }
}
