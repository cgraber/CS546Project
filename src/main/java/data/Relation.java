package data;

import utils.Consts;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class Relation {
    private EntityMention e1;
    private EntityMention e2;
    private String type;

    public Relation(String type, EntityMention e1, EntityMention e2) {
        this.e1 = e1;
        this.e2 = e2;
        this.type = type;
    }

    public EntityMention getArg1() {
        return e1;
    }

    public EntityMention getArg2() {
        return e2;
    }

    public String getType() {
        return type;
    }

    public boolean equals(Relation other) {
        if (!type.equals(other.type)) {
            return false;
        } else if (type.equals(Consts.NO_REL)) {
            // The only symmetric relation is the "No relation" relation
            if (e1.equals(other.e1) && e2.equals(other.e2)) {
                return true;
            } else if (e1.equals(other.e2) && e2.equals(other.e1)) {
                return true;
            } else {
                return false;
            }
        } else {
            if (e1.equals(other.e1) && e2.equals(other.e2)) {
                return true;
            } else {
                return false;
            }
        }
    }
}
