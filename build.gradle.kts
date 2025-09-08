plugins {
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
}

group = "dev.paraplan"
version = "0.1.2"
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Важно: чтобы не притащить старые версии testcontainers транзитивно
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.testcontainers")
    }

    // Добавляем модуль, в котором живёт @ServiceConnection
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Выравниваем версии Testcontainers через BOM
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.1"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
}

tasks.test { useJUnitPlatform() }
