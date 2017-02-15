package edu.yeditepe.discovery;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import edu.yeditepe.nlp.TurkishNLP;
import edu.yeditepe.typeclassifier.TypeClassifier;

public class CandidateEntityClassifier {
    private static final Logger LOGGER = Logger.getLogger(CandidateEntityClassifier.class);

    private static int vectorSize = 200;

    private static int featuresSize = 664;

    private static int suffixSize = 64;

    private static List<String> suffixes;

    private static LinkedTreeMap<String, double[]> vectors;

    private static Map<Integer, List<MyFeature[]>> data = new HashMap<Integer, List<MyFeature[]>>();

    public static void main(String[] args) throws Exception {
        
    //    String predictt = TypeClassifier.getInstance().predict("Murat", "Murat Amerika'nın başkanıdır.");
    
        Reader reader = new FileReader("entitySentences_tr.json");
        List<CandidateEntity> candidateEntities = new Gson().fromJson(reader, new TypeToken<List<CandidateEntity>>() {
        }.getType());
        System.out.println("Input is loaded ");
        for (CandidateEntity candidateEntity : candidateEntities) {
            try {
                System.out.println("___ "+candidateEntity.getName() );
        
            String predict = TypeClassifier.getInstance().predict(candidateEntity.getName(), candidateEntity.getSentencesContainingEntity());
            System.out.println(candidateEntity.getName() + "\t" + predict);
            LOGGER.info("\t" + candidateEntity.getName() + "\t" + predict);
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

    }

    private static double[] getAverageVector(Object[] objects) {
        double[] avg = new double[vectorSize];
        List<double[]> vectors = new ArrayList<double[]>();
        for (int i = 0; i < objects.length; i++) {
            try {
                double[] wordVector = getWordVector((String)objects[i]);
                // System.out.println(objects[i]+" "+wordVector.length+" "+wordVector[0]);
                if (wordVector != null) {
                    vectors.add(wordVector);
                }
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        if (!vectors.isEmpty()) {
            for (int i = 0; i < avg.length; i++) {
                for (int j = 0; j < vectors.size(); j++) {
                    avg[i] += (double)vectors.get(j)[i];
                }
                avg[i] /= vectors.size();
            }
            return avg;
        } else {
            return null;
        }

    }

    private static double[] getWordVector(String word) {

        double[] rawVector = null;
        rawVector = vectors.get(word);

        if (rawVector == null) {
            rawVector = vectors.get(TurkishNLP.toLowerCase(word));
        }
        if (rawVector != null) {
            return rawVector;
        }
        return null;
    }

    private static double[] getSuffixVector(Set<String> s) {
        double[] vector = new double[suffixSize];
        int i = 0;
        for (String suffix : suffixes) {
            if (s.contains(suffix)) {
                vector[i++] = 1;
            } else {
                vector[i++] = 0;
            }
        }
        return vector;
    }
}
