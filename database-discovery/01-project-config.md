# DBSM1 PROJECT ARCHITECTURE CONFIGURATION

This document details the configuration metadata of the current persistence layer.

## Architecture settings

* **Java Version:** JDK 25
* **Spring Boot Version:** 4.0.6
* **Hibernate Version:** 6.x
* **Flyway Migration:** Enabled (`spring-boot-starter-flyway`)
* **ddl-auto configuration:** `validate`
* **Naming Strategy:** `org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl`

## Datasource configuration

* **Driver:** `com.microsoft.sqlserver.jdbc.SQLServerDriver`
* **URL:** `jdbc:sqlserver://localhost:1433;databaseName=BadmintonShopDB3;encrypt=true;trustServerCertificate=true;` (Default fallback)
* **Username:** `${DB_USERNAME}`
* **Password:** `${DB_PASSWORD}`
* **Flyway Baseline Version:** 1
* **Flyway Baseline On Migrate:** true
