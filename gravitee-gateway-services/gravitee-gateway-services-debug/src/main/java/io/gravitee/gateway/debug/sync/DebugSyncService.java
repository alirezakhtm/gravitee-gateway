/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.sync;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Organization;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class DebugSyncService extends AbstractService implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DebugSyncService.class);

    @Autowired
    private TaskScheduler scheduler;

    @Value("${services.debug.cron:*/5 * * * * *}")
    private String cronTrigger;

    @Value("${services.debug.enabled:true}")
    private boolean enabled;

    @Value("${services.local.enabled:false}")
    private boolean localRegistryEnabled;

    @Autowired
    private DebugSyncManager debugSyncManager;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private GatewayConfiguration configuration;

    @Autowired
    private Node node;

    private ScheduledFuture<?> schedule;

    @Override
    protected void doStart() throws Exception {
        if (!localRegistryEnabled) {
            if (enabled) {
                super.doStart();

                logger.info("Sync debug service has been initialized with cron [{}]", cronTrigger);

                schedule = scheduler.schedule(this, new CronTrigger(cronTrigger));
            } else {
                logger.warn("Sync service is disabled");
            }
        } else {
            logger.warn("Sync debug service is disabled because local registry mode is enabled");
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (schedule != null) {
            schedule.cancel(true);
        }
        super.doStop();
    }

    @Override
    public void run() {
        debugSyncManager.refresh(new ArrayList<>((Set<String>) node.metadata().get(Node.META_ENVIRONMENTS)));
    }

    @Override
    protected String name() {
        return "Gateway Debug Sync Service";
    }
}
