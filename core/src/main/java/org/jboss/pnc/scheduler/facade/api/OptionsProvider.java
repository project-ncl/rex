package org.jboss.pnc.scheduler.facade.api;

import org.jboss.pnc.scheduler.dto.responses.LongResponse;

public interface OptionsProvider {

    void setConcurrency(Long amount);

    LongResponse getConcurrency();
}
