package learn;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by daeyun on 4/1/16.
 */
public class FeatureVectorTest {

    @After
    public void tearDown() {
        FeatureVector featureVector = new FeatureVector();
        featureVector.getFeatureMap().clear();
        featureVector.getLabelMap().clear();
    }


    @Test
    public void getOneHotLabelShouldReturnOneHot() throws Exception {
        List<FeatureVector> features = new ArrayList<>();
        features.add(new FeatureVector());
        features.add(new FeatureVector());

        features.get(0).addLabel("First");
        features.get(1).addLabel("Second");

        assertEquals(1, features.get(0).getLabelVector().get(0).intValue());
        assertEquals(0, features.get(0).getLabelVector().get(1).intValue());
        assertEquals(0, features.get(1).getLabelVector().get(0).intValue());
        assertEquals(1, features.get(1).getLabelVector().get(1).intValue());
    }

    @Test
    public void getFeaturesShouldMaintainFullLength() throws Exception {
        List<FeatureVector> features = new ArrayList<>();
        features.add(new FeatureVector());
        features.add(new FeatureVector());
        features.add(new FeatureVector());

        assertEquals(0, features.get(0).getFeatures().size());
        assertEquals(0, features.get(1).getFeatures().size());
        assertEquals(0, features.get(2).getFeatures().size());

        features.get(0).addBinaryFeature("First");

        assertEquals(1, features.get(0).getFeatures().size());
        assertEquals(1, features.get(1).getFeatures().size());
        assertEquals(1, features.get(2).getFeatures().size());

        features.get(1).addBinaryFeature("Second");
        features.get(1).addBinaryFeature("Second");

        assertEquals(2, features.get(0).getFeatures().size());
        assertEquals(2, features.get(1).getFeatures().size());
        assertEquals(2, features.get(2).getFeatures().size());

        // Back to 0. Added non-sequentially.
        features.get(0).addBinaryFeature("Third");

        assertEquals(3, features.get(0).getFeatures().size());
        assertEquals(3, features.get(1).getFeatures().size());
        assertEquals(3, features.get(2).getFeatures().size());
    }

    @Test
    public void getFeatureCountShouldReturnGlobal() throws Exception {
        List<FeatureVector> features = new ArrayList<>();
        features.add(new FeatureVector());
        features.add(new FeatureVector());
        features.get(0).addBinaryFeature("First");

        assertEquals(1, features.get(0).getFeatureCount());
        assertEquals(1, features.get(1).getFeatureCount());

        features.get(1).addBinaryFeature("Second");
        features.get(1).addBinaryFeature("Second");

        assertEquals(2, features.get(0).getFeatureCount());
        assertEquals(2, features.get(1).getFeatureCount());
    }

    @Test
    public void getFeaturesShouldReturnCorrectVectors() throws Exception {
        List<FeatureVector> features = new ArrayList<>();
        features.add(new FeatureVector());
        features.add(new FeatureVector());
        features.get(0).addBinaryFeature("First");

        assertEquals(1, features.get(0).getFeatures().get(0).intValue());
        assertEquals(0, features.get(1).getFeatures().get(0).intValue());

        features.get(1).addBinaryFeature("Second");

        assertEquals(0, features.get(0).getFeatures().get(1).intValue());
        assertEquals(1, features.get(1).getFeatures().get(1).intValue());
    }

}