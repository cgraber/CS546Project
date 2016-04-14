package experiments;

import data.ACEAnnotation;
import data.DataUtils;


import java.io.IOException;
import java.util.*;
import java.lang.*;

/**
 * Created by sdq on 3/23/16.
 */
public class DataStorage{

    public static void main(String [] argv) throws IOException {
        List<List<ACEAnnotation>> splits = DataUtils.loadDataSplits("./ACE05_English");
        ACEAnnotation.writeAlltoFile(splits);
    }




}
