package edu.yeditepe.experiment;

import it.cnr.isti.hpc.dexter.rest.domain.AnnotatedDocument;
import it.cnr.isti.hpc.dexter.util.DexterLocalParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import edu.yeditepe.controller.AnnotationController;

public class ExperimentRuntime {
	private static final Logger LOGGER = Logger
			.getLogger(ExperimentRuntime.class);

	public static void main(String[] args) {
	    Map<Integer, Long> durations= new TreeMap<Integer, Long>();

		BufferedReader in;
		DexterLocalParams params = new DexterLocalParams();

		params.addParam("text", "istanbul arsenal pas ");
		AnnotatedDocument ad = AnnotationController.annotate(params,
				"hazirlama", "1000000", null, null, null, null, "text", "0",
				"tr");

		File file = new File("experiment_runtime2");

		// Reading directory contents
		File[] files = file.listFiles();

		for (int i = 0; i < files.length; i++) {
			try {

				String urltitle = files[i].getName();

				LOGGER.info("Filename:" + urltitle);
				in = new BufferedReader(new InputStreamReader(
						(new FileInputStream(files[i]))));

				String text = "";
				String line;
				while ((line = in.readLine()) != null) {
					try {
						text += line + " ";
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
		          long endTime =0;
		          long duration =0;
		          long startTime=System.currentTimeMillis();
				params.addParam("text", text);
				ad = AnnotationController.annotate(params, text, "1000000",
						null, null, null, null, "text", "0", "tr");
				  endTime = System.currentTimeMillis();
                  duration = (endTime - startTime);
                  durations.put(StringUtils.countMatches(text, " "), duration);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		for (Integer wnum : durations.keySet()) {
		    LOGGER.info("\t"+wnum+"\t"+durations.get(wnum));
        }

	}

}
