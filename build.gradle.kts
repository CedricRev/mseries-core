plugins {
    `java-library`
    `maven-publish`
}

group = "me.cedricrev.mseries"
// Manual CI runs pass -PdevBuild=<short-sha> to get a <version>-dev+<sha> test build.
version = "1.0.0" + (findProperty("devBuild")?.let { "-dev+$it" } ?: "")

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://libraries.minecraft.net/")
}

java {
    // JDK 25 is only needed to read the Paper 26.x API class files; bytecode stays Java 21.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

// Two source sets enforce the "Spigot first" rule at compile time:
// - main only sees the Spigot API, so it cannot touch Paper or Folia classes.
// - paper sees the Paper API and is only loaded reflectively when Paper's command API exists.
// Both use the same packages so the Paper classes can stay package-private.
val paper: SourceSet = sourceSets.create("paper") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations.named(paper.compileOnlyConfigurationName) {
    extendsFrom(configurations.api.get())
}

// Paper 26.x publishes its API as Java 25 only, so this classpath must accept Java 25 variants.
configurations.named(paper.compileClasspathConfigurationName) {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.brigadier)

    // Exposed to plugins, which shade and relocate them together with core.
    api(libs.cloud.core)
    api(libs.cloud.annotations)
    api(libs.cloud.paper)
    api(libs.adventure.api)
    api(libs.adventure.minimessage)
    api(libs.adventure.legacy)
    api(libs.adventure.plain)
    api(libs.adventure.platform.bukkit)
    api(libs.hikari)

    // Tests run the storage layer against a real SQLite file.
    testImplementation(libs.spigot.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testRuntimeOnly(libs.sqlite.jdbc)

    "paperCompileOnly"(libs.paper.api)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(paper.output)
}

tasks.named<Jar>("sourcesJar") {
    from(paper.allSource)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "mseries-core"
            from(components["java"])
        }
    }
}
