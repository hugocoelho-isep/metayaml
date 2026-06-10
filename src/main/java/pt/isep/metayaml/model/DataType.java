package pt.isep.metayaml.model;

/**
 * Primitive data types that can be inferred from YAML scalar values.
 */
public enum DataType {
    STRING,
    INTEGER,
    FLOAT,
    BOOLEAN,
    NULL,
    UNKNOWN,
    /** Open map: any string key maps to a string value (e.g. {@code env}, {@code with}). */
    MAP
}
