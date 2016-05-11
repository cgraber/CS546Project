package data;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake EntityMention used for testing. Does not depend on ACEAnnotation.
 * Created by daeyun on 4/22/16.
 */
public class EntityMentionStub extends EntityMention {
    public EntityMentionStub(int id) {
        super("", "", "", id, id + 1, id, id + 1, 0, null);
    }

    @Override
    public List<String> getExtent() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getHead() {
        return new ArrayList<>();
    }
}
