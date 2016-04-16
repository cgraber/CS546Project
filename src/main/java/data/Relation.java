package data;

import utils.Consts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Colin Graber on 3/18/16.
 */
public class Relation implements Serializable {

    private static final long serialVersionUID = 4L;
    private static List<String> stringList;
    private static Map<String, Integer> labelMap;

    static{

        labelMap = new HashMap<>();
        stringList=new ArrayList<>();

        labelMap.put("NO_RELATION" ,0);
        labelMap.put("GEN-AFF", 1);
        labelMap.put("PER-SOC", 2);
        labelMap.put("ORG-AFF", 3);
        labelMap.put("PHYS", 4);
        labelMap.put("PART-WHOLE", 5);
        labelMap.put("ART" ,6);

        stringList.add("NO_RELATION");
        stringList.add("GEN-AFF");
        stringList.add("PER-SOC");
        stringList.add("ORG-AFF");
        stringList.add("PHYS");
        stringList.add("PART-WHOLE");
        stringList.add("ART");

    }

    private EntityMention e1;
    private EntityMention e2;

    public String type;
    public int type_num;

    public Relation(String type, EntityMention e1, EntityMention e2) {
        this.e1 = e1;
        this.e2 = e2;
        this.type = type;
    }

    public void SetRelation(int type_num){
        this.type_num=type_num;
        this.type = stringList.get(type_num);
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
