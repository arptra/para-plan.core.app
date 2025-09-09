plugins {
    id("java")
}

group = "dev.paraplan"
version = "0.1.0"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework:spring-context:6.1.8")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}
