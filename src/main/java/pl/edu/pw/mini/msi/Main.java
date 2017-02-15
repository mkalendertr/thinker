//package pl.edu.pw.mini.msi;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.encog.ml.data.basic.BasicMLData;
//
//public class Main {
//
//	public Main() {
//	}
//
//	public static void main(String[] args) {
//		Visualizer visualizer = new Visualizer();
//		NetworkFactory networkFactory = new NetworkFactory();
//		String outputDir = "output/";
//		FileManager fileManager = new FileManager(outputDir, outputDir);
//
//		Trainer trainer = new Trainer(0.01, 0.1, 100, 1000);
//		List<Integer> encoderHiddenLayersSizes = new ArrayList<Integer>();
//		encoderHiddenLayersSizes.add(5);
//		encoderHiddenLayersSizes.add(3);
//		encoderHiddenLayersSizes.add(2);
//		encoderHiddenLayersSizes.add(1);
//		double[][] trainingData = new double[2][7];
//		double[][] validationData = new double[2][7];
//
//		AutoEncoder2 autoEncoder = new AutoEncoder2(encoderHiddenLayersSizes,
//				trainingData, validationData, networkFactory, trainer,
//				visualizer, fileManager, "1");
//		autoEncoder.train();
//		double[]
//		new BasicMLData();
//		 autoEncoder.compute( as double[])), 0.01);
//
//	}
// }
