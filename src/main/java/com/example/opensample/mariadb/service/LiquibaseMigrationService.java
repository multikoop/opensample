package com.example.opensample.mariadb.service;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LiquibaseMigrationService {

    private final DataSource dataSource;
    private final LiquibaseProperties liquibaseProperties;
    private final ResourceLoader resourceLoader;
    private final ReentrantLock migrationLock = new ReentrantLock();

    public LiquibaseMigrationService(
            DataSource dataSource,
            LiquibaseProperties liquibaseProperties,
            ResourceLoader resourceLoader
    ) {
        this.dataSource = dataSource;
        this.liquibaseProperties = liquibaseProperties;
        this.resourceLoader = resourceLoader;
    }

    public void runMigrations() throws LiquibaseException {
        if (!migrationLock.tryLock()) {
            throw new MigrationAlreadyRunningException();
        }

        try {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setResourceLoader(resourceLoader);
            liquibase.setChangeLog(liquibaseProperties.getChangeLog());
            liquibase.setContexts(toCsv(liquibaseProperties.getContexts()));
            liquibase.setLabelFilter(toCsv(liquibaseProperties.getLabelFilter()));
            liquibase.setDefaultSchema(liquibaseProperties.getDefaultSchema());
            liquibase.setLiquibaseSchema(liquibaseProperties.getLiquibaseSchema());
            liquibase.setLiquibaseTablespace(liquibaseProperties.getLiquibaseTablespace());
            liquibase.setDatabaseChangeLogTable(liquibaseProperties.getDatabaseChangeLogTable());
            liquibase.setDatabaseChangeLogLockTable(liquibaseProperties.getDatabaseChangeLogLockTable());
            liquibase.setDropFirst(liquibaseProperties.isDropFirst());
            liquibase.setShouldRun(true);
            liquibase.afterPropertiesSet();
        } finally {
            migrationLock.unlock();
        }
    }

    private String toCsv(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return String.join(",", values);
    }
}
