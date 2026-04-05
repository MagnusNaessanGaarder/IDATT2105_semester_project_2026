package com.example.InternalControl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry
 *
 * @author Anine Harald Magnus Tri
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class InternalControlApplication {

  public static void main(String[] args) {
    SpringApplication.run(InternalControlApplication.class, args);
  }

}
