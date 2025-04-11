package com.kyohwee.batch.project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/*
Spring Batch 내부적으로 JPA를 사용하지 않는다.

Spring Batch는 배치 작업 결과를 저장하기 위해 테이블에 데이터를 넣어야 해.

그런데 Spring Batch는 그 테이블들에 접근할 때 JPA(EntityManager) 대신
JDBC를 직접 사용해.

즉, Spring Batch가 JPA 기반으로 동작하는 구현체를 안 만들었어.

그래서 MetaDB는 무조건 JDBC 기반으로 설정해줘야 함.
 */

//1번째 DB 설정
@Configuration
public class MetaDBConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource-meta")
    public DataSource metaDBSource() {

        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager metaTransactionManager() {

        return new DataSourceTransactionManager(metaDBSource());
    }
}