package pt.isep.metayaml.model;

/**
 * Represents a primitive attribute of a {@link MetaClass}.
 *
 * <p>Corresponds to a YAML key whose value is a scalar (string, integer,
 * boolean, float, or null).
 *
 * <p>Examples from Docker Compose:
 * <pre>
 *   image: postgres:16       → name="image",  type=STRING,  optional=false
 *   replicas: 3              → name="replicas", type=INTEGER, optional=false
 *   restart: unless-stopped  → name="restart", type=STRING,  optional=true
 * </pre>
 */
public class MetaAttribute {
    private final String name;
    private DataType type;
    private boolean optional;
    private boolean many; // true if this attribute appears inside a list
    private int occurrences; // how many times seen across documents

    public MetaAttribute(String name, DataType type, boolean optional, boolean many) {
        if(name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attribute name must not be blank");
        }
        this.name = name;
        this.type = type;
        this.optional = optional;
        this.many = many;
        this.occurrences = 1;
    }

    // Getters
    public String getName() { return name; }
    public DataType getType() { return type; }
    public boolean isOptional() { return optional; }
    public boolean isMany() { return many; }
    public int getOccurrences() { return occurrences; }

    // Setters - used during the refinement rules

    public void setType(DataType type) { this.type = type; }
    public void setOptional(boolean optional) { this.optional = optional; }
    public void setMany(boolean many) { this.many = many; }
    public void incrementOccurrences() { this.occurrences++; }

    @Override
    public String toString() {
        String multiplicity = many ? "[0..*]" : (optional ? "[0..1]" : "[1..1]");
        return name + " : " + type + " " + multiplicity;
    }
}
