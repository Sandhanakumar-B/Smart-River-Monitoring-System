package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"service", "dao", "dao.jpa", "controller", "main"})
@EntityScan(basePackages = "model")
@EnableJpaRepositories(basePackages = "dao.jpa")
public class SmartRiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartRiverApplication.class, args);
        System.out.println("=================================================================");
        System.out.println("  Smart River Management REST API is Running!                    ");
        System.out.println("  Day 14: Spring Data JPA & H2 Database Integration Active       ");
        System.out.println("  H2 Console : http://localhost:8080/h2-console                  ");
        System.out.println("  API Base   : http://localhost:8080/api                         ");
        System.out.println("=================================================================");
    }
}
