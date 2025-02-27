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
package io.gravitee.gateway.services.sync.apikeys.task;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.services.sync.apikeys.ApiKeysCache;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyRefresher implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyRefresher.class);

    private static final int TIMEFRAME_BEFORE_DELAY = 10 * 60 * 1000;
    private static final int TIMEFRAME_AFTER_DELAY = 1 * 60 * 1000;

    private ApiKeyRepository apiKeyRepository;

    private ClusterManager clusterManager;

    private ApiKeysCache cache;

    private final Api api;

    private Collection<String> plans;

    private long lastRefreshAt = -1;

    private long minTime;

    private long maxTime;

    private long avgTime;

    private long totalTime;

    private long count;

    private long errorsCount;

    private Throwable lastException;

    private boolean distributed;

    public ApiKeyRefresher(final Api api) {
        this.api = api;
    }

    public void initialize() {
        this.plans =
            api
                .getPlans()
                .stream()
                .filter(
                    plan ->
                        io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY.name().equalsIgnoreCase(plan.getSecurity())
                )
                .map(Plan::getId)
                .collect(Collectors.toList());
    }

    @Override
    public void run() {
        if (!plans.isEmpty() && (clusterManager.isMasterNode() || (!clusterManager.isMasterNode() && !distributed))) {
            long start = System.currentTimeMillis();
            long nextLastRefreshAt = System.currentTimeMillis();
            logger.debug("Refresh api-keys for API [name: {}] [id: {}]", api.getName(), api.getId());

            final ApiKeyCriteria.Builder criteriaBuilder;

            if (lastRefreshAt == -1) {
                criteriaBuilder = new ApiKeyCriteria.Builder().includeRevoked(false).plans(plans);
            } else {
                criteriaBuilder =
                    new ApiKeyCriteria.Builder()
                        .plans(plans)
                        .includeRevoked(true)
                        .from(lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                        .to(nextLastRefreshAt + TIMEFRAME_AFTER_DELAY);
            }

            try {
                apiKeyRepository.findByCriteria(criteriaBuilder.build()).forEach(this::saveOrUpdate);

                lastRefreshAt = nextLastRefreshAt;
            } catch (Exception ex) {
                errorsCount++;
                logger.error("Unexpected error while refreshing api-keys", ex);
                lastException = ex;
            }

            count++;

            long end = System.currentTimeMillis();

            long diff = end - start;
            totalTime += diff;

            if (count == 1) {
                minTime = diff;
            } else {
                if (diff > maxTime) {
                    maxTime = diff;
                }

                if (diff < minTime) {
                    minTime = diff;
                }
            }

            avgTime = totalTime / count;
        }
    }

    private void saveOrUpdate(ApiKey apiKey) {
        if (apiKey.isRevoked() || apiKey.isPaused()) {
            cache.remove(apiKey);
        } else {
            cache.put(apiKey);
        }
    }

    public Api getApi() {
        return api;
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }

    public long getCount() {
        return count;
    }

    public long getMinTime() {
        return minTime;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public long getAvgTime() {
        return avgTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public long getErrorsCount() {
        return errorsCount;
    }

    public Throwable getLastException() {
        return lastException;
    }

    public void setApiKeyRepository(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public void setCache(ApiKeysCache cache) {
        this.cache = cache;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }
}
