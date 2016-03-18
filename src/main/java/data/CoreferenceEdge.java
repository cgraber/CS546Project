package data;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class CoreferenceEdge {
    private EntityMention e1;
    private EntityMention e2;

    public CoreferenceEdge(EntityMention e1, EntityMention e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public Pair<EntityMention, EntityMention> getEntityMentions() {
        return new Pair<>(e1, e2);
    }

    // Note that coreference is symmetric - argument order doesn't matter
    public boolean equals(CoreferenceEdge other) {
        if (this.e1.equals(other.e1) && this.e2.equals(other.e2)) {
            return true;
        } else if (this.e2.equals(other.e1) && this.e1.equals(other.e2)) {
            return true;
        } else {
            return false;
        }
    }
}