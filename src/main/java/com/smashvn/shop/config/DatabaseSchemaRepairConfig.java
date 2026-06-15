package com.smashvn.shop.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.api.FlywayException;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DatabaseSchemaRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            log.info("[STARTUP_DB_REPAIR] Starting Flyway migration strategy...");

            // 1. Repair Flyway schema history table (to align checksums and clean failed migrations)
            try {
                log.info("[STARTUP_DB_REPAIR] Repairing Flyway schema history...");
                flyway.repair();
            } catch (FlywayException e) {
                log.error("[STARTUP_DB_REPAIR] Failed to repair Flyway schema history: {}", e.getMessage(), e);
            }

            // 2. Programmatically check and add missing columns to TaiKhoan if needed
            // This is a safety measure in case the database is out of sync or Hibernate validation is triggered early.
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                log.info("[STARTUP_DB_REPAIR] Checking TaiKhoan table schema...");

                // Add la_khach_hang column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'la_khach_hang') "
                        + "BEGIN "
                        + "    ALTER TABLE TaiKhoan ADD la_khach_hang BIT NULL; "
                        + "END"
                );

                // Add la_nhan_vien column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'la_nhan_vien') "
                        + "BEGIN "
                        + "    ALTER TABLE TaiKhoan ADD la_nhan_vien BIT NULL; "
                        + "END"
                );

                // Add la_quan_ly column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('TaiKhoan') AND name = 'la_quan_ly') "
                        + "BEGIN "
                        + "    ALTER TABLE TaiKhoan ADD la_quan_ly BIT NULL; "
                        + "END"
                );

                // Populate columns with initial values based on legacy 'vai_tro' if they are NULL
                stmt.execute(
                        "UPDATE TaiKhoan "
                        + "SET la_khach_hang = CASE WHEN vai_tro = 'KH' THEN 1 ELSE 0 END "
                        + "WHERE la_khach_hang IS NULL"
                );

                stmt.execute(
                        "UPDATE TaiKhoan "
                        + "SET la_nhan_vien = CASE WHEN vai_tro = 'NV' THEN 1 ELSE 0 END "
                        + "WHERE la_nhan_vien IS NULL"
                );

                stmt.execute(
                        "UPDATE TaiKhoan "
                        + "SET la_quan_ly = CASE WHEN vai_tro = 'QL' THEN 1 ELSE 0 END "
                        + "WHERE la_quan_ly IS NULL"
                );

                log.info("[STARTUP_DB_REPAIR] TaiKhoan table schema checks completed.");

                log.info("[STARTUP_DB_REPAIR] Checking HoaDon table schema...");

                // Add ghi_chu column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghi_chu') "
                        + "BEGIN "
                        + "    ALTER TABLE HoaDon ADD ghi_chu NVARCHAR(500) NULL; "
                        + "END"
                );

                // Add ma_giao_dich column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ma_giao_dich') "
                        + "BEGIN "
                        + "    ALTER TABLE HoaDon ADD ma_giao_dich NVARCHAR(100) NULL; "
                        + "END"
                );

                // Add nguoi_xac_nhan_thanh_toan column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'nguoi_xac_nhan_thanh_toan') "
                        + "BEGIN "
                        + "    ALTER TABLE HoaDon ADD nguoi_xac_nhan_thanh_toan NVARCHAR(100) NULL; "
                        + "END"
                );

                // Add thoi_gian_xac_nhan column if missing
                stmt.execute(
                        "IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'thoi_gian_xac_nhan') "
                        + "BEGIN "
                        + "    ALTER TABLE HoaDon ADD thoi_gian_xac_nhan DATETIME NULL; "
                        + "END"
                );

                log.info("[STARTUP_DB_REPAIR] HoaDon table schema checks completed.");
            } catch (SQLException e) {
                log.error("[STARTUP_DB_REPAIR] Error during programmatic schema updates: {}", e.getMessage(), e);
            }

            // 3. Perform standard Flyway migrations
            try {
                log.info("[STARTUP_DB_REPAIR] Migrating Flyway migrations...");
                flyway.migrate();
                log.info("[STARTUP_DB_REPAIR] Flyway migrations completed successfully.");
            } catch (FlywayException e) {
                log.error("[STARTUP_DB_REPAIR] Flyway migration failed: {}", e.getMessage(), e);
                throw e; // rethrow to stop startup if migration cannot succeed
            }
        };
    }
}
