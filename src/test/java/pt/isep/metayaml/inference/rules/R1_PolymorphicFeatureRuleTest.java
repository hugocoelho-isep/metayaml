package pt.isep.metayaml.inference.rules;

import org.junit.jupiter.api.Test;
import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link R1_PolymorphicFeatureRule}, which generalises a feature that
 * appears as both a scalar and a nested object into an abstract supertype with
 * two concrete subtypes (the GitHub Actions {@code environment} case).
 */
class R1_PolymorphicFeatureRuleTest {

    private final R1_PolymorphicFeatureRule rule = new R1_PolymorphicFeatureRule();

    @Test
    void scalarAndObjectFormBecomeAbstractWithTwoSubtypes() {
        InferredMetamodel mm = new InferredMetamodel("github-actions");

        // object form: environment: { name, url }
        MetaClass environment = mm.getOrCreateClass("Environment");
        environment.addAttribute(new MetaAttribute("name", DataType.STRING, false, false));
        environment.addAttribute(new MetaAttribute("url", DataType.STRING, true, false));

        // owner with BOTH a reference and a same-named scalar attribute
        MetaClass job = mm.getOrCreateClass("Job");
        job.addReference(new MetaReference("environment", environment, true, false, false));
        job.addAttribute(new MetaAttribute("environment", DataType.STRING, false, false)); // scalar form

        rule.apply(mm);

        // supertype kept its name, is now abstract and emptied
        MetaClass supertype = mm.findClass("Environment").orElseThrow();
        assertTrue(supertype.isAbstract(), "Environment must become abstract");
        assertTrue(supertype.getAttributes().isEmpty(), "Environment features moved to the object subtype");

        // object subtype carries the original features and extends the supertype
        MetaClass objectSub = mm.findClass("EnvironmentObject").orElseThrow();
        assertSame(supertype, objectSub.getSuperType());
        assertTrue(objectSub.findAttribute("name").isPresent());
        assertTrue(objectSub.findAttribute("url").isPresent());

        // value subtype carries a single mandatory value of the scalar's type
        MetaClass valueSub = mm.findClass("EnvironmentValue").orElseThrow();
        assertSame(supertype, valueSub.getSuperType());
        MetaAttribute value = valueSub.findAttribute("value").orElseThrow();
        assertEquals(DataType.STRING, value.getType());
        assertFalse(value.isOptional(), "value is always present in the scalar form");

        // the owner's scalar attribute is gone; the reference still targets the abstract type
        assertTrue(job.findAttribute("environment").isEmpty(), "scalar attribute removed");
        assertSame(supertype, job.findReference("environment").orElseThrow().getTarget());
    }

    @Test
    void listAndObjectFormBecomeAbstractWithManyValueSubtype() {
        InferredMetamodel mm = new InferredMetamodel("github-actions");

        // object form: on: { push: ..., issues: ... } — represented here by a couple of refs
        MetaClass on = mm.getOrCreateClass("On");
        MetaClass push = mm.getOrCreateClass("Push");
        on.addReference(new MetaReference("push", push, true, false, false));
        on.addAttribute(new MetaAttribute("branchProtectionRule", DataType.STRING, true, false));

        // owner (root) with BOTH the reference and a same-named LIST attribute (many=true)
        MetaClass workflow = mm.getOrCreateClass("GithubActions");
        workflow.addReference(new MetaReference("on", on, true, false, false));
        workflow.addAttribute(new MetaAttribute("on", DataType.STRING, false, true)); // list form: on: [push, ...]

        rule.apply(mm);

        // supertype kept its name, is now abstract and emptied
        MetaClass supertype = mm.findClass("On").orElseThrow();
        assertTrue(supertype.isAbstract(), "On must become abstract");
        assertTrue(supertype.getAttributes().isEmpty() && supertype.getReferences().isEmpty(),
                "On features moved to the object subtype");

        // object subtype carries the original structure and extends the supertype
        MetaClass objectSub = mm.findClass("OnObject").orElseThrow();
        assertSame(supertype, objectSub.getSuperType());
        assertTrue(objectSub.findReference("push").isPresent());
        assertTrue(objectSub.findAttribute("branchProtectionRule").isPresent());

        // value subtype carries a MANY-valued value, preserving the list multiplicity
        MetaClass valueSub = mm.findClass("OnValue").orElseThrow();
        assertSame(supertype, valueSub.getSuperType());
        MetaAttribute value = valueSub.findAttribute("value").orElseThrow();
        assertEquals(DataType.STRING, value.getType());
        assertTrue(value.isMany(), "value must stay many-valued for the list form (events as EString[*])");

        // the owner's list attribute is gone; the reference still targets the abstract type
        assertTrue(workflow.findAttribute("on").isEmpty(), "list attribute removed");
        assertSame(supertype, workflow.findReference("on").orElseThrow().getTarget());
    }

    @Test
    void emptyObjectFormIsLeftForEmptyClassRemoval() {
        InferredMetamodel mm = new InferredMetamodel("github-actions");

        // object form has NO structure of its own (parse artefact)
        MetaClass repository = mm.getOrCreateClass("Repository");

        MetaClass with = mm.getOrCreateClass("With");
        with.addReference(new MetaReference("repository", repository, true, false, false));
        with.addAttribute(new MetaAttribute("repository", DataType.STRING, false, false));

        rule.apply(mm);

        // no union is built: the conflict is left for R5_EmptyClassRemovalRule to collapse
        assertFalse(repository.isAbstract());
        assertTrue(mm.findClass("RepositoryObject").isEmpty());
        assertTrue(mm.findClass("RepositoryValue").isEmpty());
        assertTrue(with.findAttribute("repository").isPresent(), "scalar attribute is preserved");
    }
}
