package tn.lamg.gmao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class CliniqueGmaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CliniqueGmaoApplication.class, args);
    }
}
