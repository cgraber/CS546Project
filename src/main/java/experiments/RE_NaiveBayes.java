package experiments;

import learn.FeatureVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

import static java.lang.Math.log;

/**
 * Created by sdq on 3/23/16.
 */
public class RE_NaiveBayes {

    public static void main(String [] argv) throws IOException {


        List<FeatureVector> raw_data_set=RelationExtraction.FeaturesGenerator();
        Collections.shuffle(raw_data_set);


        List<FeatureVector> data_set=new ArrayList<>();

        //discard unbalanced data
        int keep_count=1500;
        for(FeatureVector data:raw_data_set){
            if(data.getLabel()==0) {
                if(keep_count>0) {
                    data_set.add(data);
                    keep_count--;
                }
            }
            else {
                data_set.add(data);
            }
        }




        int labels_count=data_set.get(0).getLabelCount();
        int features_count=data_set.get(0).getFeatureCount();

        int instances_size=data_set.size();
        int train_size=(instances_size*4)/5;

        System.out.println("\nsummary on instances:");
        System.out.println("instance_size:"+instances_size);
        System.out.println("train_size:"+train_size);
        System.out.println("features_size:"+features_count);
        System.out.println("labels_size:"+labels_count);


        int [] category_count=new int[labels_count];
        int [] category_count_c=new int[labels_count];
        int [] train_count=new int [labels_count];


        for(int i=0;i<instances_size;i++){
            FeatureVector f=data_set.get(i);
            int label=f.getLabel();
            category_count[label]++;
            category_count_c[label]++;
        }

        for(int i=0;i<train_size;i++){
            FeatureVector f=data_set.get(i);
            int label=f.getLabel();
            train_count[label]++;

        }


        System.out.println("\nCategory count:");

        for(int i=0;i<labels_count;i++){
            category_count_c[i]=(category_count_c[i]*4)/5;
            System.out.println(i+": "+category_count[i]);

        }



        List<FeatureVector> train_set=new ArrayList<>();
        List<FeatureVector> test_set=new ArrayList<>();


        for(FeatureVector f:data_set){

            int label = f.getLabel();
            if(category_count_c[label]>0){
                train_set.add(f);
                category_count_c[label]--;
            }
            else{
                test_set.add(f);
            }

        }


        double [] balance_factors= new double [labels_count];

        int base_index=0;
        int base_count=category_count[0];
        for(int i=0; i<labels_count;i++){
            if(category_count[i]>base_count) {
                base_index=i;
                base_count=category_count[i];
            }
        }

        balance_factors[base_index]=1.0f;

        System.out.println("\nbalance_factors");
        for(int i=0;i<labels_count;i++){
            balance_factors[i]=(float)base_count/category_count[i];
            System.out.println(balance_factors[i]);
        }

        System.out.println("train_set_size_after_balance:"+train_set.size());







        int[][] frequency_table = new int[labels_count][features_count];
        double[][] score_table = new double[labels_count][features_count];

        int[] frequency_prior=new int[labels_count];
        double[] score_prior=new double[labels_count];


        //training by counting
        for(int i=0;i<train_set.size();i++) {

            FeatureVector f=train_set.get(i);
            List<Integer> vector = f.getFeatures();
            int label = f.getLabel();
            frequency_prior[label]++;

            for (int j = 0; j < vector.size(); j++) {
                if (vector.get(j) == 1) {
                    frequency_table[label][j]++;
                }
            }
        }

        //setting up score table for future prediction
        for(int i=0; i<labels_count;i++){
            for(int j=0;j<features_count;j++){
                double frequency_score=(float)(frequency_table[i][j]+1)*balance_factors[i];
                score_table[i][j] = log(frequency_score / (train_size + features_count));
            }
        }

        //setting up score for prior
        for(int i=0;i<labels_count;i++){
            score_prior[i]=log((float)frequency_prior[i]/train_size);
        }

















        //testing
        int hit_count=0;
        int [] category_count_test=new int[labels_count];
        int [] category_hit=new int[labels_count];
        int [] category_positive_hit=new int[labels_count];


        for(int i=0;i<test_set.size();i++){

            FeatureVector f=test_set.get(i);
            double [] score_class= new double [labels_count];

            //prediction
            List<Integer> vector=f.getFeatures();
            for(int j=0;j<vector.size();j++){
                if(vector.get(j)==1){
                    for(int k=0;k<labels_count;k++){
                        score_class[k]+=score_table[k][j];
                    }
                }
            }

            for(int j=0;j<labels_count;j++){
                score_class[j]+=score_prior[j];
            }

            int prediction = 0;
            double max=score_class[0];

            for(int j=0;j<labels_count;j++){
                if(max<score_class[j]){
                    prediction=j;
                    max=score_class[j];
                }
            }

            int label =f.getLabel();
            category_count_test[label]++;
            category_hit[prediction]++;


            if(prediction==label){
                hit_count++;
                category_positive_hit[label]++;

            }

        }



        //report
        System.out.println("\nSummary: ");
        System.out.println("Overall acc:"+(float)hit_count/(instances_size-train_size));

        for(int i=0;i<labels_count;i++){

            System.out.print("Class "+i+": ");
            System.out.print("prior_frequency:"+frequency_prior[i]+"   ");
            System.out.print("hit:"+category_hit[i]+"   ");
            System.out.print("test_count:"+category_count_test[i]+"   ");
            System.out.print("corret_hit:"+category_positive_hit[i]+"   ");
            System.out.println("accuracy:"+(float)category_positive_hit[i]/category_count_test[i]);

        }






    }




}
