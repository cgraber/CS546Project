package data;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.io.Serializable;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class CoreferenceEdge implements Serializable {

    private static final long serialVersionUID = 2L;

    public EntityMention e1;
    public EntityMention e2;
    public boolean isCoreferent;
    public boolean isCoreferentLR = false;
    public boolean isCoreferentStitched = false;
    public boolean isCoreferentILP = false;
    public boolean isCoreferentILPInput = false;

    public double score;
    public ACEAnnotation sourceDocument;

    public CoreferenceEdge(EntityMention e1, EntityMention e2) {
        this(e1, e2, true);
    }

    public CoreferenceEdge(EntityMention e1, EntityMention e2, boolean isCoreferent) {
        this.e1 = e1;
        this.e2 = e2;
        this.isCoreferentLR = isCoreferent;
    }

    public boolean isCoreferent() {
        return isCoreferent;
    }

    public Pair<EntityMention, EntityMention> getEntityMentions() {
        return new Pair<>(e1, e2);
    }

    // Note that coreference is symmetric - argument order doesn't matter
    public boolean equals(CoreferenceEdge other) {
        if (this.e1.equalsCoarseExtent(other.e1) && this.e2.equalsCoarseExtent(other.e2)) {
            return true;
        } else if (this.e2.equalsCoarseExtent(other.e1) && this.e1.equalsCoarseExtent(other.e2)) {
            return true;
        } else {
            return false;
        }
    }
}
