package gitlet;
import java.io.*;
//import java.nio.file.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
//import java.lang.Thread.*;




public class Main<T> {

    public static void main(String... args) {
        try {
            Thread.sleep(400);
            branches.putAll(Utils.getBranches());
            String command = args[0];
            if (command.equals("init")) {
                init();
            } else if (command.equals("add")) {
                add(args[1]);
            } else if (command.equals("commit")) {
                if (args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                    return;
                }
                commit(args[1]);
            } else if (command.equals("rm")) {
                rm(args[1]);
            } else if (command.equals("log")) {
                log();
            } else if (command.equals("global-log")) {
                globalLog();
            } else if (command.equals("find")) {
                find(args[1]);
            } else if (command.equals("status")) {
                status();
            } else if (command.equals("branch")) {
                branch(args[1]);
            } else if (command.equals("rm-branch")) {
                rmBranch(args[1]);
            } else if (command.equals("reset")) {
                reset(args[1]);
            } else if (command.equals("merge")) {
                merge(args[1]);
            } else if (command.equals("checkout")) {
                if (args.length == 2) {
                    checkout(args[1]);
                } else {
                    if (args[1].equals("--")) {
                        checkoutfile(args[2]);
                    } else if (args[2].equals("--")) {
                        checkout(args[1], args[3]);
                    } else {
                        System.out.println("Incorrect operands");
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * structure <"HEAD" : -active branch-, -branch name- : SHA-1 Hash ... ></"HEAD">
     */
    private static HashMap<String, String> branches = new HashMap<>();
    private static String myAddr = System.getProperty("user.dir");
    private static String stagingPath = myAddr + "/.gitlet/Staging";
    private static String commitPath = myAddr + "/.gitlet/Commits";


    public static void init() {
        try {
            String gitletPath = System.getProperty("user.dir") + "/.gitlet/";
            Files.createDirectory(Paths.get(gitletPath));
            Files.createDirectory(Paths.get(gitletPath + "Staging/"));
            Files.createDirectory(Paths.get(gitletPath + "Commits/"));
            branches.put("HEAD", "master");
            branches.put("master", null);
            PrintWriter branchWriter1 = new PrintWriter(myAddr + "/.gitlet/removedFiles.txt");
            branchWriter1.print("");
            branchWriter1.close();
            Commit first = new Commit(branches, "initial commit");
            first.addCommit();
            branches.put("master", first.getCommitNumber());
            PrintWriter branchWriter = new PrintWriter(myAddr + "/.gitlet/branches.txt");
            branchWriter.print(branches.toString());
            branchWriter.close();

        } catch (IOException E) {
            System.out.println("A gitlet version-control system "
                    + "already exists in the current directory.");
        }

    }

    public static void add(String fileName) {
        String gitletPath = System.getProperty("user.dir") + "/";
        File stage = new File(gitletPath + fileName);
        File newFileLoc = new File(gitletPath
                + "/.gitlet/Staging/" + fileName);   //Path to new file
        if (!stage.exists()) {
            System.out.println("File does not exist!");
            return;
        } else {
            // check whether file has changed since last commit
            String curCommit = branches.get(branches.get("HEAD"));
            Commit c = new Commit(curCommit); //create commit object
            String stageSHA = Utils.sha1(Utils.readContents(stage));
            ArrayList<String> rmfiles = Utils.getRemovedFilesName();
            try {
                if (rmfiles.contains(fileName)) {
                    rmfiles.remove(fileName);
                    if (rmfiles.size() == 0) {
                        Utils.clearRemovedFiles();
                    } else {
                        for (String filename : rmfiles) {
                            BufferedWriter writer = new BufferedWriter(new FileWriter(
                                    myAddr + "/.gitlet/removedFiles.txt", true));
                            writer.write("REMOVED:" + fileName + "\n");
                            writer.close();
                        }
                    }
                }
                if (c.getFileMap().size() == 0
                        || !stageSHA.equals(((String) c.getFileMap().get(fileName)))) {
                    Files.copy(stage.toPath(), newFileLoc.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void rm(String fileName) {
        String curCommit = branches.get(branches.get("HEAD"));
        Commit c = new Commit(curCommit);
        //If committed, Delete in the working directory
        if (c.getFileMap().containsKey(fileName)) {
            File toBeRmv = new File(myAddr + "/" + fileName);
            toBeRmv.delete();
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(
                        myAddr + "/.gitlet/removedFiles.txt", true));
                writer.write("REMOVED:" + fileName + "\n");
                writer.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Delete in Staging Area
        File folder = new File(stagingPath);
        File[] listOfFiles = folder.listFiles();
        for (File a : listOfFiles) {
            if (a.getName().equals(fileName)) {
                a.delete();
                return;
            }
        }
        System.out.println("No reason to remove the file.");
    }


    public static <T> void commit(String message) {
        try {
            Commit a = new Commit(branches, message);
            a.getFilesandDelete();
            a.addCommit();
            PrintWriter branchWriter = new PrintWriter(myAddr + "/.gitlet/branches.txt");
            branches.put(branches.get("HEAD"), a.commitNumber);
            branchWriter.print(branches.toString());
            branchWriter.close();
        } catch (FileNotFoundException E) {
            System.out.println("gitlet not initialized.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
    }


    public static void log() {
        Commit curCommit = new Commit(branches.get(branches.get("HEAD")));
        while (true) {
            if (curCommit.getParentCommitNumber().equals("null")) {
                System.out.println(curCommit.toString());
                return;
            } else {
                System.out.println(curCommit.toString());
                curCommit = new Commit(curCommit.getParentCommitNumber()); // RETRIEVE PARENT COMMIT
            }
        }
    }


    public static void globalLog() {
        File folder = new File(commitPath);
        File[] listofFiles = folder.listFiles();
        for (File a: listofFiles) {
            String b = a.getName();
            b = b.substring(0, b.length() - 4);
            Commit c = new Commit(b);
            System.out.println(c.toString());
        }
    }

    public static void find(String msg) {
        String[] message = {msg};
        Commit a = new Commit(message);
        ArrayList<String> commitNumbers = a.findCommitsBycommitMessage();
        if (commitNumbers.isEmpty()) {
            System.out.println("Found no commit with that message.\n");
        } else {
            for (String commits : commitNumbers) {
                System.out.println(commits);
            }
        }
    }

    public static void status() {
        //Branches
        System.out.println("=== Branches ===");
        System.out.println("*" + branches.get("HEAD"));
        for (String a : branches.keySet()) {
            if (!a.equals("HEAD") && !a.equals(branches.get("HEAD"))) {
                System.out.println(a);
            }
        }
        System.out.println("\n=== Staged Files ===");
        //Staged Files
        File folder = new File(stagingPath);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> stageNames = new ArrayList<>();
        for (File a : listOfFiles) {
            stageNames.add(a.getName());
        }
        Collections.sort(stageNames);
        for (String a : stageNames) {
            System.out.println(a);
        }
        //RM
        System.out.println("\n=== Removed Files ===");
        ArrayList<String> rmNameList = Utils.getRemovedFilesName();
        Collections.sort(rmNameList);
        for (String rmname : rmNameList) {
            System.out.println(rmname);
        }
        //Modifications Not Staged
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        //Untracked Files
        System.out.println("\n=== Untracked Files ===");
    }


    public static void branch(String branchname) {
        if (branches.containsKey(branchname)) {
            System.out.println("a branch with that name already exists");
        } else {
            try {
                branches.put(branchname, branches.get(branches.get("HEAD")));
                PrintWriter branchWriter = new PrintWriter(myAddr + "/.gitlet/branches.txt");
                branchWriter.print(branches.toString());
                branchWriter.close();
            } catch (FileNotFoundException E) {
                System.out.println("gitlet not initialized");
            }
        }
    }

    public static void rmBranch(String branchname) {
        if (!branches.containsKey(branchname)) {
            System.out.println("branch with that name does not exist.");
        } else if (branches.get("HEAD") == branchname) {
            System.out.println("Cannot remove the current branch.");
        } else {
            try {
                branches.remove(branchname);
                PrintWriter branchWriter = new PrintWriter(myAddr + "/.gitlet/branches.txt");
                branchWriter.print(branches.toString());
                branchWriter.close();
            } catch (FileNotFoundException E) {
                System.out.println("gitlet not initialized");
            }
        }

    }

    public static void reset(String commitID) {
        String commitNo = Utils.findCommitExists(commitID);
        if (commitNo != null) {
            Commit c = new Commit(branches.get(branches.get("HEAD")));
            Commit d = new Commit(commitNo);
            if (Utils.findUntrackedFile(c, d)) {
                for (Object copyfilename : d.getFileMap().keySet()) {
                    Utils.copyFilebyFilename(d, (String) copyfilename);
                }
            }
            File folder = new File(stagingPath);
            File[] filelist = folder.listFiles();
            for (File a: filelist) {
                a.delete();
            }
            Utils.clearRemovedFiles();
            Utils.updateBranchPointer(branches, commitID);
        } else {
            System.out.println("No commit with that id exists.");
        }
    }


    public static void merge(String branchname) {
        int numRemoved = Utils.getRemovedFilesName().size();
        File folder = new File(stagingPath);
        File[] listOfFiles = folder.listFiles();
        if (!branches.containsKey(branchname)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        Commit myBranch = new Commit(branches.get(branches.get("HEAD")));
        Commit otherBranch = new Commit(branches.get(branchname));
        boolean conflicthappened = false;

        //first, Fail Cases
        if (listOfFiles.length != 0 || numRemoved != 0) {
            System.out.println("You have uncommitted changes.");
        } else if (branches.get("HEAD").equals(branchname)) {
            System.out.println("Cannot merge a branch with itself.");
        } else if (!(Utils.findUntrackedFile(myBranch, otherBranch))) {
            return;
        } else {
            String splitCommitNo = Utils.findSplitCommit(myBranch, otherBranch);
            Commit splitCommit = new Commit(splitCommitNo);
            Map<String, String> myMap = myBranch.getFileMap();
            Map<String, String> otherMap = otherBranch.getFileMap();
            Map<String, String> splitMap = splitCommit.getFileMap();

            if (splitCommitNo.equals(otherBranch.getCommitNumber())) {
                //Case 1  Split point is the given branch
                System.out.println("Given branch is an ancestor of the current branch.");
                return;
            } else if (splitCommitNo.equals(myBranch.getCommitNumber())) {
                //Case 2 split point is current branch
                Utils.updateBranchPointer(branches, myBranch.getCommitMessage());
                System.out.println("Current branch fast-forwarded.");
                return;
            } else {
                for (String a : splitMap.keySet()) {
                    //Absent in the given branch and unmodified in current branch
                    if (!otherMap.containsKey(a) && myMap.containsKey(a)
                            && myMap.get(a).equals(splitMap.get(a))) {
                        rm(a);
                    } else if (myMap.containsKey(a)
                            && otherMap.containsKey(a) //Modified in given branch
                            && myMap.get(a).equals(splitMap.get(a)) //but not in current branch
                            && !otherMap.get(a).equals(splitMap.get(a))) {
                        checkout(otherBranch.getCommitNumber(), a);
                        add(a);
                    } else if ((!myMap.containsKey(a)
                            && otherMap.containsKey(a)
                            && !otherMap.get(a).equals(splitMap.get(a)))
                            || (myMap.containsKey(a)
                            && !otherMap.containsKey(a)
                            && !myMap.get(a).equals(splitMap.get(a)))
                            || (myMap.containsKey(a)
                            && otherMap.containsKey(a)
                            && !myMap.get(a).equals(splitMap.get(a))
                            && !otherMap.get(a).equals(splitMap.get(a))
                            && !myMap.get(a).equals(otherMap.get(a)))) {
                        //Rasie Conflict
                        mergeConflict(myBranch, otherBranch, a);
                        conflicthappened = true;
                    }
                }
                for (String b : otherMap.keySet()) {
                    if (!splitMap.containsKey(b)    //Not present at the split point or myMap
                            && !myMap.containsKey(b)) {  //Present only in the given branch
                        checkout(otherBranch.getCommitNumber(), b);
                        add(b);
                    } else if ((myMap.containsKey(b) && // Not present in the split point
                            !splitMap.containsKey(b) && //Changed in both commits  Conflict error
                            !otherMap.get(b).equals(myMap.get(b)))) {
                        mergeConflict(myBranch, otherBranch, b);
                        conflicthappened = true;
                    }
                }
                if (!conflicthappened) {
                    commit("Merged " + branches.get("HEAD") + " with " + branchname + ".");
                }
            }
        }
    }




    private static void mergeConflict(Commit current, Commit other, String filename) {
        String mergeerror = mergeErrorString(current, other, filename);
        byte[] filecontent = mergeerror.getBytes();
        File tocopyfile = new File(myAddr + "/" + filename);
        Utils.writeContents(tocopyfile, filecontent);
        System.out.println("Encountered a merge conflict.");
        return;
    }


    public static String mergeErrorString(Commit headCommit, Commit otherCommit, String filename) {
        String headpointer = "<<<<<<< HEAD\n";
        String divider = "=======\n";
        String endpointer = ">>>>>>>\n";
        String filecontent1;
        String filecontent2;
        String myCommitFile = (String) headCommit.getFileMap().get(filename);
        if (myCommitFile != null) {
            File file1 = new File(myAddr + "/.gitlet/" + myCommitFile + ".txt");
            filecontent1 = new String(Utils.readContents(file1));
        } else {
            filecontent1 = "";
        }
        String otherCommitFile = (String) otherCommit.getFileMap().get(filename);
        if (otherCommitFile != null) {
            File file2 = new File(myAddr + "/.gitlet/" + otherCommitFile + ".txt");
            filecontent2 = new String(Utils.readContents(file2));
        } else {
            filecontent2 = "";
        }
        return headpointer + filecontent1 + divider + filecontent2 + endpointer;

    }


    public static void checkoutfile(String filename) {
        HashMap<String, String> branches1 = Utils.getBranches();
        String curCommit = branches1.get(branches1.get("HEAD"));
        Commit c = new Commit(curCommit);
        Utils.copyFilebyFilename(c, filename);
    }

    public static void checkout(String commitNum, String filename) {
        String commitNo = Utils.findCommitExists(commitNum);
        if (commitNo != null) {
            Commit c = new Commit(commitNo);
            Utils.copyFilebyFilename(c, filename);
        } else {
            System.out.println("No commit with that id exists.");
        }
    }


    public static void checkout(String commitBranch) {
        HashMap<String, String> branches2 = Utils.getBranches();
        if (!(branches2.containsKey(commitBranch))) {
            System.out.println("No such branch exists.");
            return;
        } else if (branches2.get("HEAD").equals(commitBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        } else {
            Commit c = new Commit(branches2.get(branches2.get("HEAD")));
            Commit newBranchcommit = new Commit(branches2.get(commitBranch));
            Map<String, String> newbranchmap = newBranchcommit.getFileMap();
            if (Utils.findUntrackedFile(c, newBranchcommit)) {
                for (Object a : newbranchmap.keySet()) {
                    Utils.copyFilebyFilename(newBranchcommit, (String) a);
                }
                File folder = new File(myAddr);
                File[] filelist = folder.listFiles();
                for (File a: filelist) {
                    if (a.isFile() && !newbranchmap.containsKey(a.getName())
                            && c.getFileMap().containsKey(a.getName())) {
                        a.delete();
                    }
                }
                Utils.updateBranchTxt(branches2, commitBranch);
            }
        }
    }
}








