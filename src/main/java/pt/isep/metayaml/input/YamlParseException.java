package pt.isep.metayaml.input;

/**
 * Thrown when a YAML file cannot be parsed or has an unsupported structure.
 */
public class YamlParseException extends Exception {

    public YamlParseException(String message) {
        super(message);
    }

    public YamlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}