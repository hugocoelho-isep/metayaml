package pt.isep.metayaml.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class InferredMetamodel {
    private final String dslName;
    private final List<MetaClass> classes;

    public InferredMetamodel(String dslName) {
        if (dslName == null || dslName.isBlank()) {
            throw new IllegalArgumentException("DSL name must not be blank");
        }
        this.dslName = dslName;
        this.classes = new ArrayList<>();
    }

    // Classe management
    public void addClass(MetaClass metaClass) {
        this.classes.add(metaClass);
    }

    public void removeClass(MetaClass metaClass) {
        this.classes.remove(metaClass);
    }

    public Optional<MetaClass> findClass(String className) {
        return classes.stream()
                .filter(c -> c.getName().equals(className))
                .findFirst();
    }

    public MetaClass getOrCreateClass(String name) {
        return findClass(name).orElseGet(() -> {
           MetaClass created  = new MetaClass(name);
           classes.add(created);
           return created;
        });
    }

    public List<MetaClass> getClasses() {
        return Collections.unmodifiableList(classes);
    }

    // Getters
    public String getDslName() {return dslName;}

    @Override
    public String toString() {
        return "InferredMetamodel(" + dslName + ") [" + classes.size() + " classes]";
    }
}
