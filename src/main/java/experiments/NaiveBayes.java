package experiments;

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
    private int train_size;
    private int [] train_category_count;
    private float [] balance_factors;
    private int [][] frequency_table;
    private float [][] score_table;
    private float [] unseen_score;

    public void train(List<FeatureVector> data_set){

        labels_count=data_set.get(0).getLabelCount();
        features_count=data_set.get(0).getFeatureCount();
        train_size=data_set.size();

        train_category_count = new int [labels_count];
        balance_factors = new float [labels_count];
        frequency_table = new int [labels_count][features_count];
        score_table = new float [labels_count][features_count];
        unseen_score = new float [labels_count];


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

            //unseen score for new features
            double frequency_score=(float)(1)*balance_factors[i];
            unseen_score[i] = (float) log( frequency_score / (train_size + features_count));
        }


    }






    public int predict(FeatureVector f){

        final double [] score_class= new double [labels_count];

        //prediction
        List<Integer> vector=f.getFeatures();
        for(int j=0;j<vector.size();j++){
            //check if this is unseen features
            if(vector.get(j)==1){
                for(int k=0; k<labels_count; k++){
                    if(j<features_count)
                        score_class[k]+=score_table[k][j];
                    else
                        score_class[k]+=unseen_score[k];
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

        int prediction=index_array.get(0);

        return prediction;


    }









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
