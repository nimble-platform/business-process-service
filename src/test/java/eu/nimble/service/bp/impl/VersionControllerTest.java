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
        vc = new VersionController();

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
