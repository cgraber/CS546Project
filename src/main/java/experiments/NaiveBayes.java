package experiments;

import data.EntityMention;
import learn.FeatureVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.log;

/**
 * Created by sdq on 4/13/16.
 */
public class NaiveBayes {

    private int labels_count;
    private int features_count;

    private int new_features_count;
    private int train_size;

    private int [] train_category_count;
    private float [] balance_factors;
    private int [][] frequency_table;
    private float [][] score_table;


    /**
     * training on a list of featureVector
     */
    public void train(List<FeatureVector> data_set){

        labels_count=data_set.get(0).getLabelCount();
        features_count=data_set.get(0).getFeatureCount();
        train_size=data_set.size();
        new_features_count=features_count;

        train_category_count = new int [labels_count];
        balance_factors = new float [labels_count];
        frequency_table = new int [labels_count][features_count];
        score_table = new float [labels_count][features_count];


        for(int i=0;i<train_size;i++){
            int label=data_set.get(i).getLabel();
            train_category_count[label]++;
        }

        //balance step
        int base_index=0;
        int base_count=train_category_count[0];
        for(int i=0; i < labels_count;i++){
            if(train_category_count[i]>base_count) {
                base_index=i;
                base_count=train_category_count[i];
            }
        }

        balance_factors[base_index]=1.0f;
        System.out.println("\nbalance_factors");
        for(int i=0;i<labels_count;i++){
            balance_factors[i]=(float)base_count/train_category_count[i];
            System.out.println(balance_factors[i]);
        }

        System.out.println("train_set_size_after_balance:"+train_size);

        //training by counting
        for(int i=0;i<train_size;i++) {

            FeatureVector f = data_set.get(i);
            List<Integer> vector = f.getFeatures();
            int label = f.getLabel();

            for (int j = 0; j < vector.size(); j++) {
                if (vector.get(j) == 1) {
                    frequency_table[label][j]++;
                }
            }
        }

        //setting up score table for future prediction + smoothing
        for(int i=0; i < labels_count;i++){
            for(int j=0;j<features_count;j++){
                double frequency_score=(float)(frequency_table[i][j]+1)*balance_factors[i];
                score_table[i][j] = (float) log(frequency_score / (train_size + features_count));
            }
        }


    }




    /**
     * predict on one instance of FeatureVector
     */



    public double[] giveOptions(FeatureVector f){

        final double [] score_class= new double [labels_count];
        new_features_count=f.getFeatureCount();

        //prediction
        List<Integer> vector=f.getFeatures();
        for(int j=0;j<vector.size();j++){
            //check if this is unseen features
            if(vector.get(j)==1){
                for(int k=0; k<labels_count; k++){

                    //if this is a new feature, the frequency is 0
                    int frequency = 0;
                    if(j<features_count)
                        frequency=frequency_table[k][j];


                    double frequency_score=(float)(frequency+1)*balance_factors[k];
                    double score = (float) log( frequency_score / (train_size + new_features_count));
                    score_class[k] += score;
                }
            }
        }

        return score_class;

    }


    /**
     * get the most likely relation between two reference group
     */
    public static int RelationbetweenCorefGroup(List<EntityMention> g1, List<EntityMention> g2, NaiveBayes clf, int mode){

        //mode = 0 compare max, mode = 1 compare acc max
        List<FeatureVector> list_vec = new ArrayList<>();

        //generate feature vector for all possible pair between two group
        for(int i=0; i<g1.size(); i++){
            for(int j=0; j<g2.size(); j++){
                list_vec.add(REFeatures.FeatureForOneInstance(g1.get(i), g2.get(j)));
            }
        }

        //set up score table for all possible relation
        int labels_count = list_vec.get(0).getLabelCount();
        final double [] score_table = new double [labels_count];
        for(int i=0; i<labels_count; i++){
            score_table[i] = 0f;
        }

        //use the pre-train classifier to get the score for each feature vector and then sum them up by class
        for(FeatureVector f: list_vec){

            //prediction on a single feature vector
            double [] score = clf.giveOptions(f);

            for(int i=0; i<labels_count; i++){
                if(mode == 0){
                    if(score_table[i]<score[i]){
                        score_table[i]=score[i];
                    }
                }
                score_table[i] += score[i];

            }

        }

        //get ranking by sorting index
        List<Integer> index_array = new ArrayList <> ();
        for(int j=0; j < labels_count; j++){
            index_array.add(j);
        }

        Collections.sort(index_array, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (score_table[o2] > score_table[o1])
                    return 1;
                else
                    return -1;
            }
        });

        int prediction=index_array.get(0);
        return prediction;

    }



    /**
     * prediction on a list of instances, and report the result
     */

    public void test(List<FeatureVector> data_set){

        int hit_count=0;
        int test_size = data_set.size();
        int [] test_category_hit = new int [labels_count];
        int [] test_category_count = new int [labels_count];


        //rebuild score table
        int old_freatures_count = features_count;
        features_count = data_set.get(0).getFeatureCount();
        score_table = new float [labels_count][features_count];

        for(int i=0; i < labels_count;i++){
            for(int j=0;j<features_count;j++){
                double frequency_score;
                if(j<old_freatures_count)
                    frequency_score=(float)(frequency_table[i][j]+1)*balance_factors[i];
                else
                    frequency_score=(float)(1)*balance_factors[i];
                score_table[i][j] = (float) log(frequency_score / (train_size + features_count));
            }
        }

        for(int i=0;i<test_size;i++){

            FeatureVector f=data_set.get(i);
            final double [] score_class= new double [labels_count];

            //prediction
            List<Integer> vector=f.getFeatures();
            for(int j=0;j<vector.size();j++){
                //check if this is unseen features
                if(vector.get(j)==1){
                    for(int k=0; k<labels_count; k++){
                        score_class[k]+=score_table[k][j];
                    }
                }
            }

            //get ranking
            List<Integer> index_array = new ArrayList <> ();
            for(int j=0; j < labels_count; j++){
                index_array.add(j);
            }

            Collections.sort(index_array, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    if (score_class[o2] > score_class[o1])
                        return 1;
                    else
                        return -1;
                }
            });


            //verify result
            int prediction=index_array.get(0);
            int label =f.getLabel();
            if(prediction==label) {
                hit_count++;
                test_category_hit[label]++;
            }
            test_category_count[label]++;

        }
        //report
        System.out.println("\nSummary: ");
        System.out.println("Overall acc:"+(float)hit_count/(test_size));

        for(int i=0;i<labels_count;i++){

            System.out.print("Class "+i+": ");
            System.out.print("size:"+test_category_count[i]+"  ");
            System.out.print("accuracy:"+(float)test_category_hit[i]/test_category_count[i]+"  ");
            System.out.println();

        }



    }


}
