package data;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class Relation {
    private EntityMention e1;
    private EntityMention e2;
    private String type;

    public Relation(EntityMention e1, EntityMention e2, String type) {
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
}
