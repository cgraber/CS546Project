package inference;

import data.EntityMention;
import data.EntityMentionStub;
import edu.illinois.cs.cogcomp.lbjava.infer.OJalgoHook;
import edu.illinois.cs.cogcomp.lbjava.infer.BalasHook;
import edu.illinois.cs.cogcomp.lbjava.infer.ILPInference;
import data.CoreferenceEdge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by daeyun on 4/22/16.
 */
public class EnergyMinimization {
    private OJalgoHook zeroOneIlp;
    private int numVariables = 0;
    private int verbosity = ILPInference.VERBOSITY_HIGH;

    public EnergyMinimization() {
        zeroOneIlp = new OJalgoHook();
    }

    /**
     * Adds a boolean variable x_i with energy minimization constraints.
     *
     * @param defaultValue Default value (must be either 0 or 1) for x_i.
     * @param defaultCost  Cost for keeping the default value. e.g. 1 - P(x_i = defaultValue)
     * @param flipCost     Cost for flipping the default value. e.g. P(x_i = defaultValue)
     * @return i
     */
    public int addBinaryVariable(boolean defaultValue, double defaultCost, double flipCost) {
        double zeroCost = defaultCost;
        double oneCost = flipCost;
        if (defaultValue) {
            zeroCost = flipCost;
            oneCost = defaultCost;
        }
        zeroOneIlp.addDiscreteVariable(new double[]{zeroCost, oneCost});
        return numVariables++;
    }

    /**
     * Sets an upper bound for \Sum_i x_i where x_i is a boolean variable.
     *
     * @param num Maximum number of positive x_i's
     */
    public void setMaxPositiveCountConstraint(int num) {
        int[] edgeIds = new int[numVariables];
        double[] ones = new double[numVariables];
        for (int i = 0; i < numVariables; i++) {
            edgeIds[i] = i * 2 + 1;
            ones[i] = 1;
        }
        zeroOneIlp.addLessThanConstraint(edgeIds, ones, num);
    }

    /**
     * Sets a constraint that if any two of x_i, x_j, x_k are 1, the other one must also be 1.
     */
    public void setTransitivityConstraintUnordered(int i, int j, int k) {
        // If x_i=1 and x_j=1, x_k must be 1.
        zeroOneIlp.addLessThanConstraint(new int[]{2 * i + 1, 2 * j + 1, 2 * k + 1}, new double[]{1, 1, -1}, 1);
        // If x_j=1 and x_k=1, x_i must be 1.
        zeroOneIlp.addLessThanConstraint(new int[]{2 * j + 1, 2 * k + 1, 2 * i + 1}, new double[]{1, 1, -1}, 1);
        // If x_k=1 and x_i=1, x_j must be 1.
        zeroOneIlp.addLessThanConstraint(new int[]{2 * k + 1, 2 * i + 1, 2 * j + 1}, new double[]{1, 1, -1}, 1);
    }

    public void setTransitivityConstraint(int i, int j, int k) {
        // If x_i=1 and x_j=1, x_k must be 1.
        zeroOneIlp.addLessThanConstraint(new int[]{2 * i + 1, 2 * j + 1, 2 * k + 1}, new double[]{1, 1, -1}, 1);
    }

    /**
     * Solves the energy minimization problem.
     *
     * @return Array of x_i
     * @throws Exception
     */
    public int[] solve() throws Exception {
        zeroOneIlp.setMaximize(false);

        boolean isSolved = zeroOneIlp.solve();

        if (!isSolved) {
            throw new RuntimeException("Cannot solve ILP.");
        }

        int[] result = new int[numVariables];

        for (int i = 0; i < numVariables; i++) {
            boolean isPositive = zeroOneIlp.getBooleanValue(i * 2 + 1);
            if (isPositive) {
                result[i] = 1;
            } else {
                result[i] = 0;
            }
        }
//        System.out.println("Objective value: " + zeroOneIlp.objectiveValue());
        return result;
    }

}
