package com.example.InternalControl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry
 * @author Anine Harald Magnus Tri
 */
@SpringBootApplication
@EnableScheduling
public class InternalControlApplication {

  public static void main(String[] args) {
    SpringApplication.run(InternalControlApplication.class, args);
  }

}
