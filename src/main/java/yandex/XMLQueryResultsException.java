package yandex;


/**
 * Exception thrown if Yandex.XML query returns error code or if returned
 * XML-file is incorrect
 */
public class XMLQueryResultsException extends Exception {
    public static final int ParseErrorCode = 0;
    private final int errorCode;
    private final String errorString;

    /**
     * Initialize class with Yandex.XML query error code or 0 if
     * returned XML-file cannot be parsed
     * @param errorCode code of error
     */
    public XMLQueryResultsException(int errorCode, String errorString) {
        this.errorCode = errorCode;
        this.errorString = errorString;
    }

    /**
     * Returns id of Yandex search engine error after unsuccessful query to
     * Yandex.XML or 0 if returned XML-file cannot be parsed
     * @return code of error
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns string which explains the reason of fail
     * @return error reason
     */
    public String getErrorString() {
        return errorString;
    }
}
