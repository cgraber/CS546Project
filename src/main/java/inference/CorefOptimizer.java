package inference;

import data.CoreferenceEdge;
import data.EntityMentionStub;

import java.util.*;

/**
 * Created by daeyun on 4/22/16.
 */
public class CorefOptimizer extends EnergyMinimization {
    List<CoreferenceEdge> coreferences = new ArrayList<>();

    /**
     * Example usage.
     */
    public static void main(String[] argv) throws Exception {
        // Populate coreferences with fake mentions.
        ArrayList<CoreferenceEdge> coreferenceCandidates = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            boolean localPrediction = true;
            coreferenceCandidates.add(new CoreferenceEdge(
                    new EntityMentionStub(i), new EntityMentionStub(i + 1), localPrediction));
        }

        // Example starts here.
        CorefOptimizer optimizer = new CorefOptimizer();
        optimizer.addCorefCandidates(coreferenceCandidates);

        // If any two of these three are coreferent, the other one must also be coreferent.
        optimizer.setTransitivityConstraint(2, 3, 4);

        // Limits the total number of coreferences.
        optimizer.setMaxPositiveCountConstraint(4);

        optimizer.solve();

        // Print results.
        for (int i = 0; i < optimizer.getCoreferences().size(); i++) {
            System.out.println("x_" + i + " = " + optimizer.getCoreferences().get(i).isCoreferent());
        }
    }

    /**
     * Adds coreference candidates from a list.
     */
    public void addCorefCandidates(List<CoreferenceEdge> edges) {
        for (CoreferenceEdge edge : edges) {
            addCorefCandidate(edge);
        }
    }

    /**
     * Adds a coreference candidate. edge.isCoreferent() indicates a local prediction.
     */
    public void addCorefCandidate(CoreferenceEdge edge) {
        boolean isCoreferent = edge.isCoreferent();
        if (isCoreferent) {
            addBinaryVariable(true, 0, 1);
        } else {
            addBinaryVariable(false, 0, 1);
        }
        coreferences.add(edge);
    }

    public List<CoreferenceEdge> getCoreferences() {
        return coreferences;
    }

    @Override
    public int[] solve() throws Exception {
        final int[] solution = super.solve();

        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == 0) {
                coreferences.get(i).isCoreferent = false;
            }
            if (solution[i] == 1) {
                coreferences.get(i).isCoreferent = true;
            }
        }
        return solution;
    }
}
