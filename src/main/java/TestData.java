import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;


/***
 * This class is used to store TestData in a convenient way by wrapping a list of pairs linked to a problem
 */
public class TestData {
    String problemName;
    List<Pair<String, String>> dataPairs;

    public TestData() {
        this.dataPairs = new ArrayList<>();
    }

    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }

    public void addDataPair(String input, String output) {
        this.dataPairs.add(new ImmutablePair<>(input, output));
    }

    public String getProblemName() {
        return this.problemName;
    }

    public List<Pair<String, String>> getDataPairs() {
        return this.dataPairs;
    }
}
