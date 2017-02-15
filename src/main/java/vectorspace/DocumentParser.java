package vectorspace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to read documents
 * 
 * @author kalyan
 */
public class DocumentParser {

    // This variable will hold all terms of each document in an array.
    List<String[]> termsDocsArray = new ArrayList<String[]>();

    List<String> allTerms = new ArrayList<String>(); // to hold all terms

    List<double[]> tfidfDocsVector = new ArrayList<double[]>();

    HashMap hm = new HashMap();

    List<String> files = new ArrayList<String>();

    HashMap<String, Double> map = new HashMap<String, Double>();

    ValueComparator bvc = new ValueComparator(map);

    TreeMap<String, Double> sorted_map = new TreeMap<String, Double>(bvc);

    List<String> stopwords = Arrays.asList("a",
            "able",
            "about",
            "across",
            "after",
            "all",
            "almost",
            "also",
            "am",
            "among",
            "an",
            "and",
            "any",
            "are",
            "as",
            "at",
            "be",
            "because",
            "been",
            "but",
            "by",
            "can",
            "cannot",
            "could",
            "dear",
            "did",
            "do",
            "does",
            "either",
            "else",
            "ever",
            "every",
            "for",
            "from",
            "get",
            "got",
            "had",
            "has",
            "have",
            "he",
            "her",
            "hers",
            "him",
            "his",
            "how",
            "however",
            "i",
            "if",
            "in",
            "into",
            "is",
            "it",
            "its",
            "just",
            "least",
            "let",
            "like",
            "likely",
            "may",
            "me",
            "might",
            "most",
            "must",
            "my",
            "neither",
            "no",
            "nor",
            "not",
            "of",
            "off",
            "often",
            "on",
            "only",
            "or",
            "other",
            "our",
            "own",
            "rather",
            "said",
            "say",
            "says",
            "she",
            "should",
            "since",
            "so",
            "some",
            "than",
            "that",
            "the",
            "their",
            "them",
            "then",
            "there",
            "these",
            "they",
            "this",
            "tis",
            "to",
            "too",
            "twas",
            "us",
            "wants",
            "was",
            "we",
            "were",
            "what",
            "when",
            "where",
            "which",
            "while",
            "who",
            "whom",
            "why",
            "will",
            "with",
            "would",
            "yet",
            "you",
            "your");

    public void parseFiles(String filePath, String query) throws FileNotFoundException, IOException {
        File[] allfiles = new File(filePath).listFiles();
        BufferedReader in = null;
        /*
         * File dir = new File(filePath); BufferedReader reader = null; for(File fn : dir.listFiles()){ reader = new
         * BufferedReader(new FileReader(fn)); for(String line = reader.readLine(); line != null; line =
         * reader.readLine()){ for(String _word : line.split("\\W+")){ String word = _word.toLowerCase(); } } }
         */

        for (File f : allfiles) {
            files.add(f.getPath());
            if (f.getName().endsWith(".txt")) {
                in = new BufferedReader(new FileReader(f));
                StringBuilder sb = new StringBuilder();
                String s = null;
                while ((s = in.readLine()) != null) {
                    sb.append(s);
                }
                String[] tokenizedTerms = sb.toString().replaceAll("[\\W&&[^\\s]]", "").split("\\W+");
                /*
                 * List<String> wordList = Arrays.asList(tokenizedTerm); for(String term : wordList ){
                 * if(stopwords.contains(term)) wordList.remove(term); } String[] tokenizedTerms = wordList.toArray(new
                 * String[wordList.size()]);
                 */
                for (String term : tokenizedTerms) {
                    if (stopwords.contains(term))
                        continue;
                    if (!allTerms.contains(term)) { // avoid duplicate entry
                        allTerms.add(term);
                        // System.out.println("\n"+term);
                    }
                }
                termsDocsArray.add(tokenizedTerms);
            }
        }
        String[] queryTerms = query.split(" ");
        for (String term : queryTerms) {
            if (stopwords.contains(term))
                continue;
            if (!allTerms.contains(term)) {
                allTerms.add(term);
            }
        }
        termsDocsArray.add(queryTerms);

    }

    /**
     * Method to create termVector according to its tfidf score.
     */
    public void tfIdfCalculator() {
        double tf; // term frequency
        double idf; // inverse document frequency
        double tfidf; // term requency inverse document frequency
        for (String[] docTermsArray : termsDocsArray) {
            double[] tfidfvectors = new double[allTerms.size()];
            int count = 0;
            for (String terms : allTerms) {
                tf = new TfIdf().tfCalculator(docTermsArray, terms);
                idf = new TfIdf().idfCalculator(termsDocsArray, terms);
                tfidf = tf * idf;
                tfidfvectors[count] = tfidf;
                count++;
            }
            tfidfDocsVector.add(tfidfvectors); // storing document vectors;
        }
    }

    /**
     * Method to calculate cosine similarity between all the documents.
     */
    public void getCosineSimilarity() {
        for (int i = 0; i < (files.size() - 1); i++) {
            double tdidf1 = new CosineSimilarity().cosineSimilarity(tfidfDocsVector.get(i), tfidfDocsVector.get(files.size()));
            map.put(files.get(i), tdidf1);
            // System.out.println("between" + files.get(i) + "Query = " + new
            // CosineSimilarity().cosineSimilarity(tfidfDocsVector.get(i), tfidfDocsVector.get(files.size())) );
        }
        sorted_map.putAll(map);
        // System.out.println("Result =" +sorted_map);
        for (Map.Entry<String, Double> entry : sorted_map.entrySet()) {
            String key = entry.getKey();
            // List<Tuple> values = entry.getValue();
            Double values = entry.getValue();
            if (values != 0.0) {
                System.out.println("\n Key = " + key);
            }
            // System.out.println("" );
            // for(i = 0; i< values.size();i++){
            // Tuple t = values.get(i);
            // System.out.print(t.fileno + "[" + t.position + "]" );
            // }
            // System.out.println("Values = " + values + "n");
        }

    }
}

class ValueComparator implements Comparator<String> {

    Map<String, Double> base;

    public ValueComparator(Map<String, Double> base) {
        this.base = base;
    }

    // Note: this comparator imposes orderings that are inconsistent with equals.
    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}