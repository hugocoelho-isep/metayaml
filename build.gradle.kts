plugins {
    id("java")
    application
}

application {
    mainClass.set("pt.isep.metayaml.Main")
}

group = "pt.isep.metayaml"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:2.3")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // EMF - Ecore
    implementation("org.eclipse.emf:org.eclipse.emf.ecore:2.29.0")
    implementation("org.eclipse.emf:org.eclipse.emf.ecore.xmi:2.16.0")
    implementation("org.eclipse.emf:org.eclipse.emf.common:2.28.0")
}

tasks.test {
    useJUnitPlatform()
}
