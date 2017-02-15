package pl.edu.pw.mini.msi;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FileManager {
    private static final Logger LOG = Logger.getLogger(Visualizer.class.getName());

    private static final String ERROR_FILE_FORMAT_WITH_LAYER = "%s_error_%d";
    private static final String VISUALISATION_FILE_FORMAT_WITH_LAYER = "%s_visualisation_%d.dot";
    private static final String ERROR_FILE_FORMAT = "%s_error";

    private final String errorFilesPath;
    private final String visualisationFilesPath;

    public FileManager(String errorFilesPath, String visualisationFilesPath) {
        this.errorFilesPath = errorFilesPath;
        this.visualisationFilesPath = visualisationFilesPath;
    }

    public void saveErrorDataToFile(String errorString, String fileName, int layerId) {
        String errorDataFilePath = errorFilesPath.concat(String.format(ERROR_FILE_FORMAT_WITH_LAYER, fileName, layerId));
        saveDataToFile(errorString, errorDataFilePath);
    }

    public void saveVisualisationToFile(String visualisation, String fileName, int layerId) {
        String visualisationFilePath = visualisationFilesPath.concat(
                String.format(VISUALISATION_FILE_FORMAT_WITH_LAYER, fileName, layerId));
        saveDataToFile(visualisation, visualisationFilePath);
    }

    public void saveVisualisationToFile(String visualisation, String fileName) {
        String visualisationFilePath = String.format("%s%s.dot", visualisationFilesPath, fileName);
        saveDataToFile(visualisation, visualisationFilePath);
    }

    public void saveErrorDataToFile(String errorString, String fileName) {
        String errorDataFilePath = errorFilesPath.concat(String.format(ERROR_FILE_FORMAT, fileName));
        saveDataToFile(errorString, errorDataFilePath);
    }

    public <T> void saveErrorDataToCsv(Collection<List<T>> data, String fileName) {
        saveDataToFile(data.stream()
                .map(line -> line.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("\n")), errorFilesPath + fileName);
    }

    public void saveDataToFile(String dataToSave, String FilePath) {
        try {
            File dataFile = new File(FilePath);
            FileUtils.writeStringToFile(dataFile, dataToSave.toString());
        } catch (IOException ex) {
            LOG.severe(String.format("Error while writing file %s. Exception message %s", FilePath, ex.getMessage()));
        }
    }
}