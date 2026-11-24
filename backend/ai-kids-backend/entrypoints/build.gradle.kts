val springBootVersion: String by project

dependencies {
    "implementation"(project(":application"))
    "implementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    "implementation"("org.springframework.boot:spring-boot-starter-webmvc")
    "implementation"("org.springframework.boot:spring-boot-starter-validation")
    "implementation"("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
}
