package io.github.szabelin.dualchain.mint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MintServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MintServiceApplication.class, args);
    }
}
