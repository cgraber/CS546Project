package experiments;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by sdq on 3/23/16.
 */
public class Test_Playground {

    public static void main(String [] argv) throws IOException {

        PrintWriter writer=new PrintWriter("data.txt");

        writer.println("k in the beach");
        writer.println("f noise fucksi");

        writer.close();



    }

}
