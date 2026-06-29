# DBSM1 PROJECT ARCHITECTURE CONFIGURATION

This document details the configuration metadata of the current persistence layer.

## Architecture settings

* **Java Version:** Java 21 (`pom.xml` sets `<java.version>21</java.version>`)
* **Spring Boot Version:** 4.0.6
* **Hibernate Version:** 6.x
* **Flyway Migration:** Enabled (`spring-boot-starter-flyway`)
* **ddl-auto configuration:** `validate`
* **Naming Strategy:** `org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl`

## Datasource configuration

* **Driver:** `com.microsoft.sqlserver.jdbc.SQLServerDriver`
* **URL:** `jdbc:sqlserver://localhost:1433;databaseName=DBSM1;encrypt=true;trustServerCertificate=true;` (Default fallback from `application.properties`)
* **Username:** `${DB_USERNAME}`
* **Password:** `${DB_PASSWORD}`
* **Flyway Baseline Version:** 1
* **Flyway Baseline On Migrate:** true
