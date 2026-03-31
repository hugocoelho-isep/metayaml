package pt.isep.metayaml.model;

/**
 * Represents a structural reference from one {@link MetaClass} to another.
 *
 * <p>A reference is inferred when a YAML key's value is a mapping (object)
 * or a list of mappings.
 *
 * <p>Examples from GitHub Actions:
 * <pre>
 *   # Containment (owner holds target, target cannot exist without owner)
 *   jobs:
 *     build:               → Workflow --[jobs]--> Job  (containment, many=true)
 *       steps:
 *         - uses: ...      → Job --[steps]--> Step     (containment, many=true)
 *
 *   # Non-containment (cross-reference)
 *   needs: [build, test]   → Job --[needs]--> Job      (containment=false, many=true)
 * </pre>
 */
public class MetaReference {
    private final String name;
    private MetaClass target;
    private boolean containment;
    private boolean optional;
    private boolean many;
    private int occurrences;

    public MetaReference(String name, MetaClass target, boolean containment, boolean optional, boolean many) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("Reference name must not be blank");
        }
        if(target == null){
            throw new IllegalArgumentException("Reference target must not be null");
        }
        this.name = name;
        this.target = target;
        this.containment = containment;
        this.optional = optional;
        this.many = many;
        this.occurrences = 1;
    }

    // Getters
    public String getName() { return name; }
    public MetaClass getTarget() { return target; }
    public boolean isContainment() { return containment; }
    public boolean isOptional() { return optional; }
    public boolean isMany() { return many; }
    public int getOccurrences() { return occurrences; }

    // Setters - used during the refinement rules
    public void setTarget(MetaClass target) { this.target = target;}
    public void setContainment(boolean containment) { this.containment = containment; }
    public void setOptional(boolean optional) { this.optional = optional; }
    public void setMany(boolean many) { this.many = many; }
    public void incrementOccurrences(){ this.occurrences++; }

    @Override
    public String toString() {
        String multiplicity  = many ? (optional ? "[0..*]" : "[1..*]") : (optional ? "[0..1]" : "[1..1]");
        String containMarker = containment ? " <<containment>>" : "";
        return name + " --> " + target.getName() + " " + multiplicity + containMarker;
    }
}
