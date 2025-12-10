package com.app;

import com.app.config.Database;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class StudyGroupBackendApplication {

    public static void main(String[] args) {
        Database.init();
        SpringApplication.run(StudyGroupBackendApplication.class, args);
    }
}
