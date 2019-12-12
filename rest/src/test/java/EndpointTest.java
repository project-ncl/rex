import io.quarkus.test.junit.QuarkusTest;
import org.jboss.pnc.scheduler.rest.api.ServiceEndpoint;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@QuarkusTest
public class EndpointTest {

    @Inject
    ServiceEndpoint endpoint;

    @Test
    public void testMethod() {
        int asd = 3;
    }
}
