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
package io.gravitee.gateway.core.builder;

import io.gravitee.gateway.core.definition.ProxyDefinition;

import java.net.URI;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ProxyDefinitionBuilder {

    private final ProxyDefinition proxyDefinition = new ProxyDefinition();

    public ProxyDefinitionBuilder contextPath(String contextPath) {
        this.proxyDefinition.setContextPath(contextPath);
        return this;
    }

    public ProxyDefinitionBuilder target(String target) {
        this.proxyDefinition.setTarget(URI.create(target));
        return this;
    }

    public ProxyDefinitionBuilder stripContextPath(boolean stripContextPath) {
        this.proxyDefinition.setStripContextPath(stripContextPath);
        return this;
    }

    public ProxyDefinition build() {
        return this.proxyDefinition;
    }

}
