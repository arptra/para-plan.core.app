
package dev.paraplan.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import dev.paraplan.app.config.AdvisorProperties;

// Scan all project modules so that beans like SqlHintService in
// the `dev.paraplan.hints` package are registered.
@SpringBootApplication(
        scanBasePackages = "dev.paraplan",
        exclude = {DataSourceAutoConfiguration.class}
)
@EnableConfigurationProperties(AdvisorProperties.class)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
