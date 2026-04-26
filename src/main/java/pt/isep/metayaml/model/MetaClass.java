package pt.isep.metayaml.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a concept (class) in the inferred metamodel.
 *
 * <p>A MetaClass is inferred when a YAML key's value is a mapping (object).
 * It holds a set of {@link MetaAttribute}s for scalar properties and
 * {@link MetaReference}s for nested object properties.
 *
 * <p>Examples:
 * <ul>
 *   <li>GitHub Actions: {@code Workflow}, {@code Job}, {@code Step}
 *   <li>Docker Compose: {@code Service}, {@code Volume}, {@code Healthcheck}
 *   <li>Ansible: {@code Play}, {@code Task}
 * </ul>
 */
public class MetaClass {
    private final String name;
    private final List<MetaAttribute> attributes;
    private final List<MetaReference> references;
    private int occurrences; // how many times seen across documents

    public MetaClass(String name) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("MetaClass name must not be blank");
        }
        this.name = name;
        this.attributes  = new ArrayList<>();
        this.references  = new ArrayList<>();
        this.occurrences = 0;
    }


    // Attribute management
    public void addAttribute(MetaAttribute attribute) {
        attributes.add(attribute);
    }

    public Optional<MetaAttribute> findAttribute(String name) {
        return attributes.stream()
                .filter(attribute -> attribute.getName()
                .equals(name)).findFirst();
    }

    public void removeAttribute(String name) {
        attributes.removeIf(a -> a.getName().equals(name));
    }

    public void removeReference(String name) {
        references.removeIf(r -> r.getName().equals(name));
    }

    public List<MetaAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }


    // Reference management
    public void addReference(MetaReference reference){
        references.add(reference);
    }

    public Optional<MetaReference> findReference(String name) {
        return references.stream()
                .filter(reference -> reference.getName()
                .equals(name)).findFirst();
    }

    public List<MetaReference> getReferences() {
        return Collections.unmodifiableList(references);
    }

    // Occurrence tracking, using in refinement rule
    public int getOccurrences() {return occurrences; }
    public void incrementOccurrences(){ this.occurrences++; }

    // Getters
    public String getName(){ return name; }


    @Override
    public String toString(){
        return "MetaClass(" + name + ") ["
                + attributes.size() + "attrs, "
                + references.size() + " refs]";
    }


}
