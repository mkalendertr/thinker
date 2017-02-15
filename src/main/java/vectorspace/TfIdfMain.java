package vectorspace;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * 
 * @author Kalyan
 */
public class TfIdfMain {

    /**
     * Main method
     * 
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void main(String args[]) throws FileNotFoundException, IOException {
        DocumentParser dp = new DocumentParser();
        dp.parseFiles("vsm", "mobile telephone group Orange Plc said Tuesday its pre-tax losses expanded by 40 percent");
        dp.tfIdfCalculator(); // calculates tfidf
        dp.getCosineSimilarity(); // calculated cosine similarity
    }
}
