package com.dgkncgty.spring.parentcreator;

import org.apache.maven.model.Parent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class ParentCreatorApplication implements CommandLineRunner {

    @Autowired
    ParentCreatorService parentCreator;

    @Override
    public void run(String... args) throws Exception {
        parentCreator.createParentPom();
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(ParentCreatorApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }

}
