package org.jboss.pnc.rex.facade;

import org.jboss.pnc.rex.core.api.QueueManager;
import org.jboss.pnc.rex.dto.responses.LongResponse;
import org.jboss.pnc.rex.facade.api.OptionsProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@ApplicationScoped
public class OptionsProviderImpl implements OptionsProvider {

    private final QueueManager manager;

    @Inject
    public OptionsProviderImpl(QueueManager manager) {
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
