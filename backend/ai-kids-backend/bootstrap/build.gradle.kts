plugins {
    id("org.springframework.boot")
}

val springBootVersion: String by project

dependencies {
    "implementation"(project(":application"))
    "implementation"(project(":infrastructure"))
    "implementation"(project(":entrypoints"))
    "implementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    "implementation"("org.springframework.boot:spring-boot-starter-webmvc")
    "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
    "testImplementation"("com.tngtech.archunit:archunit-junit5:1.5.1")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName = "ai-kids-studio.jar"
}
