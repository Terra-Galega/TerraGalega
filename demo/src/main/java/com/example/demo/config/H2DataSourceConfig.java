package com.example.demo.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

/**
 * Base de datos PRINCIPAL (H2): todo lo que ya existía. Es @Primary, así que
 * los @Transactional y repositorios existentes la siguen usando.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.example.demo.repository", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
public class H2DataSourceConfig {

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password:}") String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("dataSource") DataSource dataSource,
            @Value("${spring.jpa.hibernate.ddl-auto:create-drop}") String ddlAuto) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.example.demo.entities");
        emf.setPersistenceUnitName("h2");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", ddlAuto);
        props.put("hibernate.format_sql", "true");
        emf.setJpaPropertyMap(props);
        return emf;
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    // Spring Boot activaba "open in view" solo; al configurar JPA a mano hay que
    // registrarlo, si no los DTO que leen listas lazy (details) fallan.
    @Bean
    public WebMvcConfigurer openInViewConfigurer(@Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                OpenEntityManagerInViewInterceptor interceptor = new OpenEntityManagerInViewInterceptor();
                interceptor.setEntityManagerFactory(emf);
                registry.addWebRequestInterceptor(interceptor);
            }
        };
    }
}