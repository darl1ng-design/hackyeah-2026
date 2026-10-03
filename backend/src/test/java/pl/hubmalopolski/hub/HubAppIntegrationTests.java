package pl.hubmalopolski.hub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@EnabledIfEnvironmentVariable(named = "HUB_INTEGRATION_TESTS", matches = "true")
@SpringBootTest(properties = "hub.seed-vectors=false")
class HubAppIntegrationTests {

    @Test
    void contextLoadsWithDatabase() {
    }
}
