package gitlet;

import org.junit.Test;

//import org.apache.commons.io.FileUtils;
import java.io.*;
import java.util.Map;

import static java.lang.Thread.sleep;

/**
 * Created by m2thomas on 7/19/17.
 */
public class MainTest {
    @Test
    public void testscript() throws Exception {
        doInit();
        String file1 = createRandomFile();
        doAdd(file1);
        doCommit("file1test");
        String commitname = Utils.getBranches().get(Utils.getBranches().get("HEAD"));
        Commit myCommit = new Commit(commitname);
        System.out.println(Main.mergeErrorString(myCommit, myCommit, file1));

    }

    @Test //This is here so I can run it individually between tests
    public void resetTestingFile()  {
        try {
            //FileUtils.deleteDirectory(new File("/Users/m2thomas/Desktop/TestingFolder"));
        } catch (Exception e){
            // Ignore exceptions
        }
    }

    public void doInit() throws InterruptedException {
        Main.main("init");
        Thread.sleep(1);
    }

    public void doAdd(String filename) throws InterruptedException {
        Main.main("add", filename);
        Thread.sleep(1);
    }

    public void doCommit(String message) throws InterruptedException {
        Main.main("commit", message);
        Thread.sleep(1);
    }

    private static final String ALPHA_NUMERIC_STRING =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()_+";


    public static String randomStringGen(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public String createRandomFile() throws FileNotFoundException{
        String filename =  randomStringGen(6);
        PrintWriter branchWriter1 = new PrintWriter(System.getProperty("user.dir")
                + "/" + filename + ".txt");
        branchWriter1.print(randomStringGen(20));
        branchWriter1.close();
        return filename + ".txt";
    }
}