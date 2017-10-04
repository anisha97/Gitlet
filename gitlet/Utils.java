package gitlet;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/** Assorted utilities.
 *  @author P. N. Hilfinger
 */
class Utils {


    public static HashMap<String, String> getBranches() {
        HashMap<String, String> returnBranches = new HashMap<>();
        File branchFile = Paths.get(System.getProperty("user.dir")
                + "/.gitlet" + "/branches.txt").toFile();
        if (branchFile.exists()) {
            byte[] brByte = Utils.readContents(branchFile);
            String mapStr = new String(brByte, StandardCharsets.UTF_8);
            if (!mapStr.equals("")) {
                mapStr = mapStr.substring(1, mapStr.length() - 1);
                String[] mapsPairs = mapStr.split(",");
                for (String s : mapsPairs) {
                    String[] keyVal = s.split("=");
                    returnBranches.put(keyVal[0].trim(), keyVal[1].trim());
                }
            }
        }
        return returnBranches;
    }

    public static ArrayList<String> getRemovedFilesName() {
        ArrayList<String> rmList = new ArrayList<>();
        File rmFile = new File(System.getProperty("user.dir")
                + "/.gitlet/removedFiles.txt");
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(rmFile));
            line = br.readLine();
            while (line != null) {
                String[] part = line.split(":");
                rmList.add(part[1]);
                line = br.readLine();
            }
            return rmList;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Something went wrong");
        } catch (IOException e) {
            throw new IllegalArgumentException("Something went wrong");
        }
    }

    public static void clearRemovedFiles() {
        try {
            File file = new File(System.getProperty("user.dir")
                    + "/.gitlet/removedFiles.txt");
            PrintWriter writer = new PrintWriter(file);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static String myAddr = System.getProperty("user.dir");
    private static String stagingPath = myAddr + "/.gitlet/Staging";
    private static String commitPath = myAddr + "/.gitlet/Commits";

    public static String findCommitExists(String commitNo) {
        File folder = new File(commitPath);
        File[] listOfFiles = folder.listFiles();
        for (File a : listOfFiles) {
            String b = a.getName();
            String checkstring = b.substring(0, commitNo.length());
            if (commitNo.equals(checkstring)) {
                return b.substring(0, b.length() - 4);
            }
        }
        return null;
    }

    public static boolean findUntrackedFile(Commit c, Commit d) {
        Map<String, String> fileMapc = c.getFileMap();
        Map<String, String> fileMapd = d.getFileMap();
        File folder = new File(myAddr);
        File[] listOfFiles = folder.listFiles();
        for (File a: listOfFiles) {
            if (a.isFile()) {
                String stageSHA = Utils.sha1(Utils.readContents(a));
                if (!a.getName().equals(".DS_Store")
                        && !(fileMapc.containsKey(a.getName()))
                        && fileMapd.containsKey(a.getName())
                        && !(stageSHA.equals(fileMapd.get(a.getName())))) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                    return false;
                }
            }
        }
        return true;
    }

    public static void updateBranchTxt(HashMap<String, String> branches, String commitBranch) {
        try {
            PrintWriter branchWriter = new PrintWriter(myAddr + "/.gitlet/branches.txt");
            branches.put("HEAD", commitBranch);
            branchWriter.print(branches.toString());
            branchWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void updateBranchPointer(HashMap<String, String> branches, String commitBranch) {
        try {
            PrintWriter branchWriter = new PrintWriter(myAddr + "/.gitlet/branches.txt");
            branches.put(branches.get("HEAD"), commitBranch);
            branchWriter.print(branches.toString());
            branchWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void copyFilebyFilename(Commit c, String filename) {
        if (c.getFileMap().containsKey(filename)) {
            String copyFileSHA = (String) c.getFileMap().get(filename);
            File file = new File(myAddr + "/.gitlet/" + copyFileSHA + ".txt");
            byte[] filecontent = Utils.readContents(file);
            File tocopyfile = new File(myAddr + "/" + filename);
            Utils.writeContents(tocopyfile, filecontent);
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }



    public static String findSplitCommit(Commit c, Commit d) {
        ArrayList<String> curCommitlists = new ArrayList<>();
        while (!c.getParentCommitNumber().equals("null")) {
            curCommitlists.add(c.getCommitNumber());
            c = new Commit(c.getParentCommitNumber());
        }
        curCommitlists.add(c.getCommitNumber());
        while (!curCommitlists.contains(d.getCommitNumber())) {
            d = new Commit(d.getParentCommitNumber());
        }
        return d.getCommitNumber();
    }






    /* SHA-1 HASH VALUES. */

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write the entire contents of BYTES to FILE, creating or overwriting
     *  it as needed.  Throws IllegalArgumentException in case of problems. */
    static void writeContents(File file, byte[] bytes) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            Files.write(file.toPath(), bytes);
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /* OTHER FILE UTILITIES */

    /** Return the concatentation of FIRST and OTHERS into a File designator,

     *  method. */
    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

}




