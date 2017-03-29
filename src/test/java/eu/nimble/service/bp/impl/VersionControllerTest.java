package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;

public class VersionControllerTest {

    private VersionController vc;

    @Before
    public void setUp() {
        vc = new VersionController("0.0.1", "business-process-service-test");

    }

    @Test
    public void testGet() {
        ResponseEntity<Version> ex = vc.versionGet();
        assertEquals("0.0.1", ex.getBody().getVersion());

    }

    @After
    public void tearDown() {
    }

}
