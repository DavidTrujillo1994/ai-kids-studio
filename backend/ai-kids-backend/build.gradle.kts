plugins {
    id("org.springframework.boot") version "4.1.3" apply false
}

val springBootVersion: String by project

subprojects {
    apply(plugin = "java-library")
    group = "com.aikids"
    version = "1.0.0"

    repositories { mavenCentral() }

    tasks.withType<JavaCompile>().configureEach {
        options.release = 21
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all,-processing,-serial,-classfile"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxHeapSize = "512m"
        defaultCharacterEncoding = "UTF-8"
    }
    dependencies {
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
        "testImplementation"("org.junit.jupiter:junit-jupiter")
        "testImplementation"("org.assertj:assertj-core")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
