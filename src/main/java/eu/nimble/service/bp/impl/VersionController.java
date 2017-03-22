package eu.nimble.service.bp.impl;

import eu.nimble.service.bp.swagger.model.Version;
import eu.nimble.service.bp.swagger.api.VersionApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class VersionController implements VersionApi {

    public ResponseEntity<Version> versionGet() {
        return new ResponseEntity<Version>(VersionFactory.create(), HttpStatus.OK);
    }

    private static class VersionFactory {
        static Version create() {
            Version v = new Version();
            v.setVersion("0.0.1");
            v.setServiceId("example");
            return v;
        }
    }
}
