import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import spoon.SpoonException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;


/***
 * This class sets up a connection to a downloaded Code4Bench and dumps the data from set queries.
 */
public class Main {
    static String db_url = "mysql://localhost:3306/code4bench";
    static String db_user = "";
    static String db_password = "";

    public static void main(String[] args) {
        setupDB();
    }
    private static void setupDB() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn =
                    DriverManager.getConnection(
                            "jdbc:" + db_url +
                            "?user=" + db_user +
                            "&password=" + db_password
                    );

            // Do something with the Connection
            stmt = conn.createStatement();

            String querySources = Files.readString(Paths.get("src/main/resources/sources.sql"));
            String queryTests = Files.readString(Paths.get("src/main/resources/tests.sql"));

            rs = stmt.executeQuery(querySources);
            dumpSourceFiles(rs);

            rs = stmt.executeQuery(queryTests);
            generateTests(listTests(rs, false));

        } catch (SQLException ex) {
            // handle any errors
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        } catch (IOException ex) {
            System.out.println("IOException: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            // it is a good idea to release
            // resources in a finally{} block
            // in reverse-order of their creation
            // if they are no-longer needed

            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) {
                } // ignore

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) {
                } // ignore

                stmt = null;
            }
        }
    }

    /***
     * Dumps source files from the database, filtering out certain files that may be problematic.
     * @param rs
     * @throws SQLException
     * @throws IOException
     */
    private static void dumpSourceFiles(ResultSet rs) throws SQLException, IOException {
        int subId;
        String sourceCode;
        String author;
        Timestamp sent;
        String language;
        String problemName;
        String className;
        int rowCount = 0;
        int normalBuggyCount = 0;
        int irregularConstructorCount = 0;
        int threadSpawnerCount = 0;
        int staticInnerClassCount = 0;
        String outputFolderPath;
        Path outputFilePath;
        String problemFolder = "c4bj/src/main/java/";

        while(rs.next()) {
            rowCount++;
            subId = Integer.parseInt(rs.getString(2));
            sourceCode = rs.getString(3);
            author = rs.getString(4);
            sent = rs.getTimestamp(5);
            language = rs.getString(6).replaceAll("\\s+","_").toLowerCase();
            problemName = rs.getString(7);

            try {
                className = Spoonery.getTrueClassName(sourceCode);

                // filtering
                if (Spoonery.hasIrregularConstructor(sourceCode)) { // filter out programs with constructors that use parameters
                    irregularConstructorCount++;
                    outputFolderPath = "c4bj/filtered_packages/irregular_constructors/" + language + "/problem_" + problemName + "/subId_" + subId + "/";
                } else if (sourceCode.contains("Thread")) { // filter out programs that spawn threads, might catch a few others, but should be rare
                    threadSpawnerCount++;
                    outputFolderPath = "c4bj/filtered_packages/spawns_threads/" + language + "/problem_" + problemName + "/subId_" + subId + "/";
                } else if (Spoonery.hasNestedStaticClass(sourceCode)) { // filter programs with static inner classes, because they are problematic to test
                    staticInnerClassCount++;
                    outputFolderPath = "c4bj/filtered_packages/static_inner/" + language + "/problem_" + problemName + "/subId_" + subId + "/";
                } else {
                    normalBuggyCount++;
                    outputFolderPath = problemFolder + language + "/problem_" + problemName + "/subId_" + subId + "/";
                }

                // transformations
                sourceCode = Spoonery.setPrivateConstructorsPublic(sourceCode);

                // output directories and source files
                Files.createDirectories(Paths.get(outputFolderPath));
                outputFilePath = Paths.get( outputFolderPath + className + ".java");
                sourceCode = "package " + language + ".problem_" + problemName + ".subId_" + subId + ";\n\n" + sourceCode;
                Files.writeString(outputFilePath, sourceCode);
            } catch (SpoonException ex) {
                System.out.println(ex.getMessage());
                System.out.println("Exception on subId: " + subId);
            }
        }
        System.out.println("Processed data rows: " + rowCount);
        System.out.println("Programs with irregular constructors: " + irregularConstructorCount);
        System.out.println("Programs that spawn threads: " + threadSpawnerCount);
        System.out.println("Programs that have inner static classes: " + staticInnerClassCount);
        System.out.println("Usable buggy programs: " + normalBuggyCount);
    }

    private static List<TestData> listTests(ResultSet rs, boolean write_to_disk) throws SQLException, IOException {
        String problemName = "";
        String problemName_old = "";
        String input;
        String expectedOutput;
        List<TestData> testDataList = new ArrayList<>();
        TestData testData = new TestData();;

        while (rs.next()) {
            problemName = rs.getString(1);
            input = rs.getString(2).replaceAll("(\\s)+", "$1"); // multiple -> single whitespace
            //input = input.replaceAll("\\R", "\\\\n");
            expectedOutput = rs.getString(3).replaceAll("(\\s)+", "$1"); // multiple -> single whitespace
            //expectedOutput = expectedOutput.replaceAll("\\R", "\\\\n");

            if (problemName.equals(problemName_old)) {
                testData.addDataPair(input, expectedOutput);
                problemName_old = problemName;
                continue;
            }

            if (!problemName_old.equals("")) {
                testDataList.add(testData);
                testData = new TestData();
            }
            testData.setProblemName(problemName);
            testData.addDataPair(input, expectedOutput);
            problemName_old = problemName;
        }

        if (write_to_disk) {
            // write to disk
        }

        return testDataList;
    }

    private static void generateTests(List<TestData> tests) throws IOException {
        String programPath = "c4bj/src/main/java/";
        File programFolder = new File(programPath);
        String problemName = "";
        String sourceCode = "";
        String testPath = "c4bj/src/test/java/";
        Files.createDirectories(Paths.get(testPath));

        for (File javaFolder : programFolder.listFiles()) {
            for (File problemFolder : javaFolder.listFiles()) {
                problemName = problemFolder.getName().substring(8);
                for (File submissionFolder : problemFolder.listFiles()) {
                    for (File file : submissionFolder.listFiles()) {
                        String filename = file.getName();

                        //if (filename.contains("_Test")) continue;

                        String className = FilenameUtils.removeExtension(filename);
                        sourceCode = Files.readString(Paths.get("src/main/resources/testTemplate-experimental.txt"));

                        sourceCode = setClassName(sourceCode, className, javaFolder.getName() + "." + problemFolder.getName() + "." + submissionFolder.getName() + ".");
                        sourceCode = setTestData(sourceCode, problemName, tests);

                        sourceCode = "package " + javaFolder.getName() + "." + problemFolder.getName() + "." + submissionFolder.getName() + ";\n\n" + sourceCode;

                        Files.createDirectories(Paths.get(testPath + javaFolder.getName() + "/" + problemFolder.getName() + "/" + submissionFolder.getName()));
                        Files.writeString(Paths.get(testPath + javaFolder.getName() + "/" + problemFolder.getName() + "/" + submissionFolder.getName() + "/" + className + "_Test.java"), sourceCode);
                    }
                }
            }
        }
    }

    private static String setClassName(String sourceCode, String className, String prefix) {
        sourceCode = sourceCode.replaceAll("<testClassName>", className + "_Test");
        sourceCode = sourceCode.replaceAll("<className>", prefix + className);

        return sourceCode;
    }

    private static String setTestData(String sourceCode, String problemName, List<TestData> tests) throws NullPointerException {
        StringBuilder dataString = new StringBuilder();
        List<Pair<String, String>> data = null;

        // choose input and output data by problemName
        for (TestData test : tests) {
            if (problemName.equals(test.getProblemName())) {
                data = test.getDataPairs();
            }
        }

        // parse input and output data
        for (Pair<String, String> dataPair : data) {
            dataString.append("\"'" + dataPair.getLeft().replaceAll( "[\"'\\\\]", "\\\\$0").replaceAll("\\R", "\\\\n") + "','" + dataPair.getRight().replaceAll( "[\"'\\\\]", "\\\\$0").replaceAll("\\R", "\\\\n")  + "'\",");
        }

        // cut off last ","
        dataString.setLength(dataString.length() - 1);

        sourceCode = sourceCode.replace("<inputOutput>", dataString.toString());

        return sourceCode;
    }

    private static void dumpOracles(ResultSet rs) {
    }
}