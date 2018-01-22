/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.integrationtests.dmn;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNResult;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.integrationtests.shared.KieServerDeployer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class DMNMapInputIntegrationTest
        extends DMNKieServerBaseIntegrationTest {

    private static final String CONTAINER_ID = "map-input";

    private static final ReleaseId kjar = new ReleaseId("org.kie.server.testing", CONTAINER_ID, "1.0.0.Final");
    
    private static final String MODEL_NAMESPACE = "http://www.trisotech.com/definitions/_0726e0ca-0f39-4aae-9abf-173fd79a66a8";
    private static final String MODEL_NAME = "map-input";

    @BeforeClass
    public static void deployArtifacts() {
        KieServerDeployer.buildAndDeployCommonMavenParent();
        KieServerDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/map-input").getFile());
    }
    
    @Before
    public void cleanContainers() {
        disposeAllContainers();
        createContainer(CONTAINER_ID, kjar);
    }

    @Override
    protected void addExtraCustomClasses(Map<String, Class<?>> extraClasses) throws Exception {
        // no extra classes.
    }

    // See org.kie.dmn.core.DMNInputRuntimeTest
    @Test
    public void test_evaluateAll() {
        DMNContext dmnContext = dmnClient.newContext();
        Map<String, Object> person = new HashMap<>();

        person.put("name", "John Doe");
        person.put("age", 47);

        dmnContext.set("Person", person);
        ServiceResponse<DMNResult> evaluateAll = dmnClient.evaluateAll(CONTAINER_ID, dmnContext);
        
        assertEquals(ResponseType.SUCCESS, evaluateAll.getType());
        
        DMNResult dmnResult = evaluateAll.getResult();
        
        assertThat( dmnResult.getDecisionResults().size(), is( 1 ) );
        assertThat(dmnResult.getDecisionResultByName("Greetings").getResult(), is("Hello John Doe"));

        DMNContext result = dmnResult.getContext();

        assertThat(result.get("Greetings"), is("Hello John Doe"));
    }
    
}