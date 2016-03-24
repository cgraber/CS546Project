package experiments;

import data.ACEAnnotation;
import data.DataUtils;
import learn.FeatureVector;
import learn.FeatureVectorSet;

import java.io.IOException;
import java.util.List;
import static java.lang.Math.log;

/**
 * Created by sdq on 3/23/16.
 */
public class RE_NaiveBayes {

    public static void main(String [] argv) throws IOException {

        FeatureVectorSet data_set_pack= FeatureVectorSet.readFromFile();
        List<FeatureVector> data_set=data_set_pack.getList();

        System.out.println("load data successfully");


        int labels_count=data_set_pack.getLabelsCount();
        int features_count=data_set_pack.getFeaturesCount();

        int instances_size=data_set.size();
        int train_size=(instances_size*4)/5;

        System.out.println(instances_size);
        System.out.println(train_size);
        System.out.println(features_count);
        System.out.println(labels_count);


        int[][] frequency_table = new int[labels_count][features_count];
        double[][] score_table = new double[labels_count][features_count];

        int[] frequency_prior=new int[labels_count];
        double[] score_prior=new double[labels_count];


        //training by counting
        for(int i=0;i<train_size;i++) {

            FeatureVector f=data_set.get(i);
            List<Integer> vector = f.getFeatures();
            int label = f.getLabel();
            frequency_prior[label]++;
            //System.out.println(i);
            for (int j = 0; j < vector.size(); j++) {
                if (vector.get(j) == 1) {
                    frequency_table[label][j]++;
                }
            }
        }


        //const
        double zero_feature_value=(float)log((double)1/(train_size+features_count));
        //System.out.println(zero_feature_value);



        //setting up score table for future prediction
        for(int i=0; i<labels_count;i++){
            for(int j=0;j<features_count;j++){
                int frequency=frequency_table[i][j];
                //System.out.println(frequency);
                if(frequency==0) {
                    score_table[i][j] = zero_feature_value;
                }
                else {
                    score_table[i][j] = log((float)(frequency + 1) / (train_size + features_count));
                    //System.out.println(score_table[i][j]);
                }
            }
        }


        //setting up score for prior
        for(int i=0;i<labels_count;i++){
            score_prior[i]=log((float)frequency_prior[i]/train_size);
            //System.out.println(score_prior[i]);
        }


        //testing


        int hit_count=0;
        for(int i=train_size;i<instances_size;i++){

            FeatureVector f=data_set.get(i);
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

            if(prediction==f.getLabel()){
                hit_count++;
            }

        }

        //report
        System.out.println((float)hit_count/(instances_size-train_size));





    }




}
