package utils;

/**
 * Holds all of the String constants used by varioius parts in this system
 *
 * Created by Colin Graber on 3/6/16.
 */
public class Consts {
    //Constituent Attributes
    public static final String ATTR_COREF_TYPE = "ATTR_COREF_TYPE";
    public static final String ATTR_COREF_LDCTYPE = "ATTR_COREF_LDCTYPE";
    public static final String ATTR_COREF_HEAD_START = "ATTR_COREF_HEAD_START";
    public static final String ATTR_COREF_HEAD_END = "ATTR_COREF_HEAD_END";
    public static final String ATTR_REL_TYPE = "ATTR_REL_TYPE";
    public static final String ATTR_REL_SUBTYPE = "ATTR_REL_SUBTYPE";
    public static final String ATTR_REL_ARG_ROLE = "ATTR_REL_ARG_ROLE";

    //View Names
    public static final String VIEW_RELATION_GOLD = "VIEW_RELATION_GOLD";
    public static final String VIEW_RELAATION = "VIEW_RELATION";
    public static final String VIEW_COREF_GOLD = "VIEW_COREF_GOLD";

    //Misc
    public static final String ARG_1 = "Arg-1";
    public static final String ARG_2 = "Arg-2";
    public static final String[] ARGS = {ARG_1, ARG_2};

    public static final String NO_REL = "NO_REL";

    public static final String BIO_B = "B_";
    public static final String BIO_I = "I_";
    public static final String BIO_O = "O";

    public static final String PRONOUN = "PRO";

    //Features
    public static final String WORD_FEATURE = "WORD_";
    public static final String PREV_FEATURE = "WORD-1_";
    public static final String WORD_PREV_FEATURE = "WORD-1:0_";
    public static final String PREV_2_WORD = "WORD-2:0_";
    public static final String POS_FEATURE = "POS_";
    public static final String POS_WORD_FEATURE = "POS:WORD_";
    public static final String CAPITALIZED = "__CAPITALIZED";
    public static final String ALL_CAPS = "__ALLCAPS";
    public static final String IN_HEAD = "_IN_HEAD";
    public static final String HEAD_OFFSET = "_HEAD_OFFSET_";
}
