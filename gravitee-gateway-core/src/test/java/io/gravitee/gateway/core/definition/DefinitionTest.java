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
package io.gravitee.gateway.core.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class DefinitionTest {

    @Test
    public void definition_defaultHttpConfig() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-defaulthttpconfig.json");

        Assert.assertEquals(URI.create("http://localhost:1234"), api.getProxy().getTarget());
        Assert.assertFalse(api.getProxy().getHttpClient().isUseProxy());
    }

    @Test
    public void definition_overridedHttpConfig() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-overridedhttpconfig.json");

        Assert.assertEquals(URI.create("http://localhost:1234"), api.getProxy().getTarget());
        Assert.assertTrue(api.getProxy().getHttpClient().isUseProxy());

        Assert.assertNotNull(api.getProxy().getHttpClient().getHttpProxy());
    }

    @Test
    public void definition_noProxyPart() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-noproxy-part.json");

        Assert.assertNull(api.getProxy());
    }

    @Test
    public void definition_noPath() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-nopath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(0, api.getPaths().size());
    }

    @Test
    public void definition_defaultPath() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-defaultpath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, PathDefinition> paths = api.getPaths();
        Assert.assertEquals("/*", paths.keySet().iterator().next());

        List<MethodDefinition> subPaths = paths.get("/*").getMethods();
        Assert.assertEquals(1, subPaths.size());
    }

    @Test
    public void definition_multiplePath() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-multiplepath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(2, api.getPaths().size());
    }

    @Test
    public void definition_pathwithmethods() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-defaultpath.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, PathDefinition> paths = api.getPaths();

        List<MethodDefinition> methodDefinitions = paths.get("/*").getMethods();
        Assert.assertEquals(1, methodDefinitions.size());

        HttpMethod[] methods = methodDefinitions.iterator().next().getMethods();
        Assert.assertEquals(2, methods.length);
    }

    @Test
    public void definition_pathwithoutmethods() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-path-nohttpmethod.json");

        Assert.assertNotNull(api.getPaths());
        Assert.assertEquals(1, api.getPaths().size());

        Map<String, PathDefinition> paths = api.getPaths();

        List<MethodDefinition> methodDefinitions = paths.get("/*").getMethods();
        Assert.assertEquals(1, methodDefinitions.size());

        HttpMethod[] methods = methodDefinitions.iterator().next().getMethods();
        Assert.assertEquals(9, methods.length);
    }

    @Test
    public void definition_pathwithpolicies() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-defaultpath.json");
        Map<String, PathDefinition> paths = api.getPaths();
        List<MethodDefinition> methodDefinitions = paths.get("/*").getMethods();

        List<PolicyDefinition> policies = methodDefinitions.iterator().next().getPolicies();
        Assert.assertEquals(2, policies.size());

        Iterator<PolicyDefinition> policyNames = policies.iterator();
        Assert.assertEquals("access-control", policyNames.next().getName());
        Assert.assertEquals("rate-limit", policyNames.next().getName());
    }

    @Test
    public void definition_pathwithpolicies_disabled() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-defaultpath.json");
        Map<String, PathDefinition> paths = api.getPaths();
        List<MethodDefinition> methodDefinitions = paths.get("/*").getMethods();

        List<PolicyDefinition> policies = methodDefinitions.iterator().next().getPolicies();
        Assert.assertEquals(2, policies.size());

        Iterator<PolicyDefinition> policyNames = policies.iterator();

        PolicyDefinition accessControlPolicy = policyNames.next();
        PolicyDefinition rateLimitPolicy = policyNames.next();

        Assert.assertEquals("access-control", accessControlPolicy.getName());
        Assert.assertFalse(accessControlPolicy.isEnabled());

        Assert.assertEquals("rate-limit", rateLimitPolicy.getName());
        Assert.assertTrue(rateLimitPolicy.isEnabled());
    }

    @Test
    public void definition_pathwithoutpolicy() throws Exception {
        ApiDefinition api = getDefinition("/io/gravitee/gateway/core/definition/api-path-withoutpolicy.json");
        Map<String, PathDefinition> paths = api.getPaths();
        List<MethodDefinition> methodDefinitions = paths.get("/*").getMethods();

        Assert.assertEquals(1, methodDefinitions.size());
    }

    private ApiDefinition getDefinition(String resource) throws Exception {
        URL jsonFile = DefinitionTest.class.getResource(resource);
        return objectMapper().readValue(jsonFile, ApiDefinition.class);
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
