package main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"service", "dao", "controller", "main"})
public class SmartRiverApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartRiverApplication.class, args);
        System.out.println("===============================================");
        System.out.println("  Smart River Management REST API is Running!  ");
        System.out.println("===============================================");
    }
}
