package experiments;

import learn.FeatureVector;
import utils.Metric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.*;

import static java.lang.Math.log;

/**
 * Created by sdq on 3/23/16.
 */
public class ReNaiveBayes {

    public static void main(String [] argv) throws IOException {


        List<FeatureVector> raw_data_set= ReFeatures.generateFeatures();

        //randomly selected training and testing set
        Collections.shuffle(raw_data_set);


        List<FeatureVector> data_set=raw_data_set;


        int numLabels=data_set.get(0).getLabelCount();
        int features_count=data_set.get(0).getFeatureCount();

        int instances_size=data_set.size();
        int train_size=(instances_size*4)/5;

        Map<String, Integer> labelMap=data_set.get(0).getLabelMap();
        List<String> StringList=data_set.get(0).getStringList();

        System.out.println("\nsummary on instances:");
        System.out.println("instance_size:"+instances_size);
        System.out.println("train_size:"+train_size);
        System.out.println("features_size:"+features_count);
        System.out.println("labels_size:"+numLabels);
        System.out.println("Labels_Categories:\n"+labelMap);




        int [] category_count=new int[numLabels];
        int [] category_count_c=new int[numLabels];
        int [] train_count=new int [numLabels];


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

        for(int i=0;i<numLabels;i++){
            category_count_c[i]=(category_count_c[i]*4)/5;
            System.out.println(i+": "+category_count[i]);

        }



        List<FeatureVector> train_set=new ArrayList<>();
        List<FeatureVector> testSet=new ArrayList<>();


        for(FeatureVector f:data_set){

            int label = f.getLabel();
            if(category_count_c[label]>0){
                train_set.add(f);
                category_count_c[label]--;
            }
            else{
                testSet.add(f);
            }

        }


        double [] balance_factors= new double [numLabels];

        int base_index=0;
        int base_count=category_count[0];
        for(int i=0; i<numLabels;i++){
            if(category_count[i]>base_count) {
                base_index=i;
                base_count=category_count[i];
            }
        }

        balance_factors[base_index]=1.0f;

        System.out.println("\nbalance_factors");
        for(int i=0;i<numLabels;i++){
            balance_factors[i]=(float)base_count/category_count[i];
            System.out.println(balance_factors[i]);
        }

        System.out.println("train_set_size_after_balance:"+train_set.size());







        int[][] frequency_table = new int[numLabels][features_count];
        double[][] score_table = new double[numLabels][features_count];

        int[] frequency_prior=new int[numLabels];
        double[] score_prior=new double[numLabels];


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

        //setting up score table for future isCoreferentLR
        for(int i=0; i<numLabels;i++){
            for(int j=0;j<features_count;j++){
                double frequency_score=(float)(frequency_table[i][j]+1)*balance_factors[i];
                score_table[i][j] = log(frequency_score / (train_size + features_count));
            }
        }

        //setting up score for prior
        for(int i=0;i<numLabels;i++){
            score_prior[i]=log((float)frequency_prior[i]/train_size);
        }

















        //testing
        int hit_count=0;
        int two_hit=0;
        int three_hit=0;

        double max_margin= 0.0;
        double max_margin2=0.0;

        int [] two_hit_category= new int [numLabels];

        double [] truePositives=new double[numLabels];
        double [] trueNegatives=new double[numLabels];
        double [] falsePositives=new double[numLabels];
        double [] falseNegatives=new double[numLabels];

        for(int i=0;i<testSet.size();i++){

            FeatureVector f=testSet.get(i);
            final double [] score_class= new double [numLabels];

            //isCoreferentLR
            List<Integer> vector=f.getFeatures();
            for(int j=0;j<vector.size();j++){
                if(vector.get(j)==1){
                    for(int k=0;k<numLabels;k++){
                        score_class[k]+=score_table[k][j];
                    }
                }
            }

            for(int j=0;j<numLabels;j++){
                //score_class[j]+=score_prior[j];
            }

            List<Integer> index_array = new ArrayList <> ();

            for(int j=0; j<numLabels; j++){
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
            int second_guess=index_array.get(1);
            int third_guess=index_array.get(2);


            //get the true label
            int label =f.getLabel();

            //summary for instance
            if(prediction==label) {
                hit_count++;
                two_hit++;
                three_hit++;

                max_margin+=(score_class[prediction]-score_class[second_guess]);

                two_hit_category[label]++;

                for (int j = 0; j < numLabels; j++) {
                    if (prediction==j){
                        truePositives[j]++;
                    } else {
                        trueNegatives[j]++;
                    }
                }
            } else {
                for (int j = 0; j < numLabels; j++) {
                    if (prediction==j) {
                        falsePositives[j]++;
                    } else if (label==j){
                        falseNegatives[j]++;
                    } else {
                        trueNegatives[j]++;
                    }
                }

                if (second_guess == label) {
                    two_hit++;
                    three_hit++;
                    two_hit_category[second_guess]++;


                    max_margin2+=(score_class[prediction]-score_class[second_guess]);

                    //show confusion
                    System.out.println("\n");
                    System.out.println("First:" + StringList.get(prediction));
                    System.out.println("Second:" + StringList.get(second_guess));
                    System.out.println("Truth:" + StringList.get(label));



                }
                if(third_guess == label){
                    three_hit++;


                    System.out.println("\n");
                    System.out.println("First:" + StringList.get(prediction));
                    System.out.println("Third:" + StringList.get(third_guess));
                    System.out.println("Truth:" + StringList.get(label));



                }
            }
        }

        double precision=0, recall=0, accuracy=0;
        for (int i = 0; i < numLabels; i++) {
            precision+=truePositives[i]/(truePositives[i]+falsePositives[i]);
            recall+=truePositives[i]/(truePositives[i]+falseNegatives[i]);
            accuracy+=truePositives[i];
        }
        precision/=numLabels;
        recall/=numLabels;
        accuracy/=testSet.size();

        Metric metric = new Metric();
        metric.setAccuracy(accuracy);
        metric.setPrecision(precision);
        metric.setRecall(recall);

        System.out.println("----\n" + metric.toString());


        //report
        System.out.println("\nSummary: ");
        System.out.println("Overall acc:"+(float)hit_count/(instances_size-train_size));
        System.out.println("Overall two_hit_acc:"+(float)two_hit/(instances_size-train_size));
        System.out.println("Overall three_hit_acc:"+(float)three_hit/(instances_size-train_size));

        System.out.println("avergae margin_first:" +(float)max_margin/hit_count);
        System.out.println("avergae margin_second:" +(float)max_margin2/two_hit);

        for(int i=0;i<numLabels;i++){
            double labelAccuracy = (truePositives[i]+trueNegatives[i])/(truePositives[i]+trueNegatives[i]+
                    falseNegatives[i]+falsePositives[i]);

            System.out.print("Class "+i+": ");
            System.out.print("prior:"+frequency_prior[i]+"  ");
            System.out.print("accuracy:"+labelAccuracy+"  ");
            System.out.println();

        }



    }




}
