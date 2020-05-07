package eu.nimble.service.bp.util;

import eu.nimble.service.bp.impl.StartWithDocumentController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CredentialsUtil {

    @Value("${nimble.default-token}")
    private String defaultToken;

    public String getBearerToken(){

        if(defaultToken != null && !defaultToken.isEmpty()){
            return defaultToken;
        }

        return StartWithDocumentController.token;
    }
}
