val springBootVersion: String by project

dependencies {
    "implementation"(project(":application"))
    "implementation"(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    "implementation"("org.springframework.boot:spring-boot-starter-flyway")
    "implementation"("org.hibernate.orm:hibernate-community-dialects")
    "implementation"("tools.jackson.core:jackson-databind")
    "runtimeOnly"("org.xerial:sqlite-jdbc")
    "testImplementation"("tools.jackson.core:jackson-databind")
}
