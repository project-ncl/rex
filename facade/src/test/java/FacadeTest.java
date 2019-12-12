import io.quarkus.test.junit.QuarkusTest;
import org.jboss.msc.service.ServiceName;
import org.jboss.pnc.scheduler.common.enums.Mode;
import org.jboss.pnc.scheduler.core.model.Service;
import org.jboss.pnc.scheduler.dto.RemoteLinksDTO;
import org.jboss.pnc.scheduler.dto.ServiceDTO;
import org.jboss.pnc.scheduler.facade.api.ServiceProvider;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;

@QuarkusTest
public class FacadeTest {

    @Inject
    ServiceProvider provider;

    @Test
    public void rip() {
        int abc = 3;

        provider.create(Arrays.asList(new ServiceDTO[]{ServiceDTO.builder().name("halo").mode(Mode.IDLE).payload("asd")
                .links(new RemoteLinksDTO("http://localhost:8080", "http://localhost:8080")).dependants(new HashSet<>()).dependencies(new HashSet<>()).build()}));

        ServiceDTO serviceDTO = provider.get(ServiceName.parse("halo"));
    }
}
