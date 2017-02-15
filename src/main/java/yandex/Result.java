package yandex;

/**
 * Immutable class which represents Yandex websearch answer for one of the
 * pages.
 */
public class Result {
    private final String url;
    private final String title;
    private final String annotation;
    private final String greenLine;

    /**
     *
     * @param url URL of the source
     * @param title title of the source page
     * @param annotation annotation of relevant information
     * @param greenLine green line on yandex search (domain name without www)
     */
    public Result(String url, String title,
                  String annotation, String greenLine) {
        this.title = title;
        this.url = url;
        this.annotation = annotation;
        this.greenLine = greenLine;
    }

    @Override
    public String toString() {
        return "Result{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", annotation='" + annotation + '\'' +
                ", greenLine='" + greenLine + '\'' +
                '}';
    }

    /**
     * Returns url of the source Yandex has found results from
     * @return source url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns title of the result
     * @return title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns annotation of the result
     * @return annotation string
     */
    public String getAnnotation() {
        return annotation;
    }

    /**
     * Returns "green line" from Yandex
     * @return green line from the results page (which is domain name without www)
     */
    public String getGreenLine() {
        return greenLine;
    }
}
