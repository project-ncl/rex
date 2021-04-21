package org.jboss.pnc.scheduler.facade;

import org.jboss.pnc.scheduler.core.api.QueueManager;
import org.jboss.pnc.scheduler.core.delegates.WithRetries;
import org.jboss.pnc.scheduler.dto.responses.LongResponse;
import org.jboss.pnc.scheduler.facade.api.OptionsProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
public class OptionsProviderImpl implements OptionsProvider {

    private final QueueManager manager;

    @Inject
    public OptionsProviderImpl(@WithRetries QueueManager manager) {
        this.manager = manager;
    }

    @Override
    @Transactional
    public void setConcurrency(Long amount) {
        manager.setMaximumConcurrency(amount);
    }

    @Override
    public LongResponse getConcurrency() {
        return LongResponse
                .builder()
                .number(manager.getMaximumConcurrency())
                .build();
    }
}
