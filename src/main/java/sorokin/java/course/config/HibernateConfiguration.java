package sorokin.java.course.config;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import sorokin.java.course.account.Account;
import sorokin.java.course.user.User;

@Configuration
public class HibernateConfiguration {

    @Bean
    public SessionFactory sessionFactory(Environment environment) {
        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();

        configuration
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Account.class)
                .setProperty(
                        "hibernate.connection.url",
                        environment.getProperty("hibernate.connection.url"))
                .setProperty(
                        "hibernate.connection.username",
                        environment.getProperty("hibernate.connection.username"))
                .setProperty(
                        "hibernate.connection.password",
                        environment.getProperty("hibernate.connection.password"))
                .setProperty(
                        "hibernate.connection.driver_class",
                        environment.getProperty("hibernate.connection.driver_class"))
                .setProperty(
                        "hibernate.dialect",
                        environment.getProperty("hibernate.dialect"))
                .setProperty(
                        "hibernate.show_sql",
                        environment.getProperty("hibernate.show_sql"))
                .setProperty(
                        "hibernate.format_sql",
                        environment.getProperty("hibernate.format_sql"))
                .setProperty(
                        "hibernate.hbm2ddl.auto",
                        environment.getProperty("hibernate.hbm2ddl.auto"));

        ServiceRegistry serviceRegistry = new
                StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();

        return configuration.buildSessionFactory(serviceRegistry);
    }
}
