package gitlet;
import java.io.*;
import java.io.File;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Created by Henryye on 7/14/17.
 */
public class Commit<T> implements Serializable {
    protected Map<String, String> fileMap;
    protected String timeStamp;
    protected String commitNumber;
    protected String parentCommitNumber;
    protected String commitMessage;
    protected String stagingPath;
    protected String commitPath;
    protected String gitletPath;
    protected ArrayList<String> removedFileNames;
    protected int removedFileLength;


    //Need to take in Message from the User
    public Commit(HashMap<String, String> parentMap, String message) {
        commitMessage = message;
        gitletPath = System.getProperty("user.dir") + "/.gitlet";
        stagingPath = gitletPath + "/Staging";
        commitPath = gitletPath + "/Commits";
        parentCommitNumber = parentMap.get(parentMap.get("HEAD"));
        removedFileNames = Utils.getRemovedFilesName();
        if (parentCommitNumber == null) {
            fileMap = new HashMap<>();
        } else {
            fileMap = new Commit(parentCommitNumber).getFileMap();
        }
        //If there is filename in the removedFile, then delete it from the
        //File map and at last clear the removedFile File
        for (String a: removedFileNames) {
            if (fileMap.containsKey(a)) {
                fileMap.remove(a);
            }
        }
        removedFileLength = Utils.getRemovedFilesName().size();
        Utils.clearRemovedFiles();


        /*HashMap<String, String> branches = new HashMap<>();
        branches.putAll(Utils.getBranches());
        parentCommitNumber = branches.get(branches.get("HEAD"));*/

    }

    public Commit(String commitNumber) {
        this.commitNumber = commitNumber;
        gitletPath = System.getProperty("user.dir") + "/.gitlet";
        commitPath = gitletPath + "/Commits/" + commitNumber + ".txt";
        fileMap = new HashMap<String, String>();
        //Update all of the attributes by the provided commitNumber
        this.updateInfo();
    }

    ///ATTENTION!!!! IN ORDER TO DISTINGUISH THE String commitNumber,
    // Pass in CommitMessage as a String array
    public Commit(String[] commitMessage) {
        this.commitMessage = commitMessage[0];
        commitPath = System.getProperty("user.dir") + "/.gitlet/Commits";
    }




    //Go to the staging directory to get the file. Copy the file to the .gitlet directory
    //Then delete all of the files in Staging dir
    // Save all of the file and their corresponding SHA value in the map

    //ATTENTION MUST USE TRY AND CATCH THE ILLEGALARGUMENTEXCEPTION


    public void getFilesandDelete() {
        File folder = new File(stagingPath);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles.length == 0 && removedFileLength == 0) {
            throw new IllegalArgumentException("No changes added to the commit.\n");
        }
        for (File file: listOfFiles) {
            byte[] tempByte = Utils.readContents(file);  //ReadFile
            String tempSHA = Utils.sha1(tempByte);    //Get SHA1
            //Create the Copy file in the .gitlet folder
            File tempfile = new File(gitletPath + "/" + tempSHA + ".txt");
            // Make a Filename - SHA Map
            fileMap.put(file.getName().toString(), tempSHA);
            //Wrtie the content of the original file to the copy file
            Utils.writeContents(tempfile, tempByte);
            file.delete();
        }
        if (folder.list().length == 0) {
            commitNumber = Utils.sha1(new SimpleDateFormat
            ("yyyy-MM-dd HH:mm:ss").format(new Date()));
            return;
        } else {
            System.out.println("Something went Wrong");
            return;
        }
    }

    //Write all of the data in the "CommitNumber".txt file
    //Including current commitNumber, Parent CommitNumber, TimeStamp, Commit Message, FileMap
    public void addCommit() {
        /* Write all of the Commit in a file:
        Format:
        First line: Commit Number
        Second line: Commit Message
        Third Line: TimeStamp
        Fourth Line: Parent Commit Number
        Fifth Line to the end: Commited File name: corresponding commit number
         */
        timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.commitNumber = Utils.sha1(timeStamp);
        try {
            File toWrite = new File(commitPath + "/" + commitNumber + ".txt");
            FileOutputStream fos = new FileOutputStream(toWrite);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(commitNumber);
            bw.newLine();
            bw.write(commitMessage);
            bw.newLine();
            bw.write(timeStamp);
            bw.newLine();
            if (parentCommitNumber == null) {
                bw.write("null");
            } else {
                bw.write(parentCommitNumber);
            }
            bw.newLine();
            for (String filename : fileMap.keySet()) {
                bw.write(filename + ":" + fileMap.get(filename));
                bw.newLine();
            }
            bw.close();
        } catch (FileNotFoundException e) {
            return;
        } catch (IOException e) {
            return;
        }
    }



    public static void main(String... args) {
        Commit a = new Commit(new HashMap<String, String>(), "FIRST TRY");
        a.getFilesandDelete();
        a.addCommit();

    }



    /////////////////////////////////////////////////////////////////////////////////////////////


    //#The rest are for given a commit number and return all of the info

    /*
    First line: Commit Number
    Second line: Commit Message
    Third Line: TimeStamp
    Fourth Line: Parent Commit Number
    Fifth Line to the end: Commited File name: corresponding commit number
    */

    public void updateInfo() {
        File file = new File(commitPath);
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(file));
            commitNumber = br.readLine();
            commitMessage = br.readLine();
            timeStamp = br.readLine();
            parentCommitNumber = br.readLine();
            line = br.readLine();
            while (line != null) {
                String[] part = line.split(":");
                fileMap.put(part[0], part[1]);
                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("No Such Commit");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //////////////////////////////////////////////////////////////////////////////////////


    /*This one is for give back a String arraylist
      contains the COMMIT IDS filtered by the commit message*/
    public ArrayList<String> findCommitsBycommitMessage() {
        File folder = new File(commitPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> toRet = new ArrayList<>();
        try {
            for (File a : listOfFiles) {
                if (!a.getName().equals(".DS_Store")) {
                    BufferedReader br = new BufferedReader(new FileReader(a));
                    String commit = br.readLine();
                    String msg = br.readLine();
                    if (msg.equals(commitMessage)) {
                        toRet.add(commit);
                    }
                }
            }
            return toRet;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return toRet;
        } catch (IOException e) {
            e.printStackTrace();
            return toRet;
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////

    //The rest are returing all of the info
    //MUST BE USED AFTER EITHER COMMIT OR UPDATEFROMFILE
    // This is unnecessary if you have the commit object from UPDATE
    public HashMap<String, T> getAllInfo() {
        HashMap<String, T> toRet = new HashMap<>();
        toRet.put("CommitNumber", (T) commitNumber);
        toRet.put("ParentCommit", (T) parentCommitNumber);
        toRet.put("CommitMessage", (T) commitMessage);
        toRet.put("TimeStamp", (T) timeStamp);
        toRet.put("CommitedMap", (T) fileMap);
        return toRet;
    }


    public Map<String, String> getFileMap() {
        return fileMap;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getCommitNumber() {
        return commitNumber;
    }

    public String getParentCommitNumber() {
        return parentCommitNumber;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public String toString() {
        return "===\n" + "Commit " + this.getCommitNumber() + "\n"
                + this.getTimeStamp() + "\n" + this.getCommitMessage() + "\n";
    }
}
