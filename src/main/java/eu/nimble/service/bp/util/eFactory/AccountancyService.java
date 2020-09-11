package eu.nimble.service.bp.util.eFactory;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import eu.nimble.service.bp.exception.NimbleExceptionMessageCode;
import eu.nimble.utility.exception.NimbleException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Component
public class AccountancyService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${efactory.service-registry.accountancy-service.url}")
    private String serviceRegistryAccountancyServiceUrl;
    @Value("${nimble.oauth.eFactoryClient.accessTokenUri}")
    private String eFactoryAccessTokenUri;
    @Value("${nimble.oauth.eFactoryClient.clientId}")
    private String eFactoryClientId;
    @Value("${nimble.oauth.eFactoryClient.clientSecret}")
    private String eFactoryClientSecret;

    private final String CLIENT_CREDENTIALS_FLOW = "client_credentials";

    public void sendPaymentLog(String paymentLog, String orderId) throws Exception{
        if(Strings.isNullOrEmpty(serviceRegistryAccountancyServiceUrl)){
            logger.info("Could not send payment log since no url set for efactory logstash");
        }
        else {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();

            map.add("grant_type", CLIENT_CREDENTIALS_FLOW);
            map.add("client_id", eFactoryClientId);
            map.add("client_secret", eFactoryClientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(eFactoryAccessTokenUri, request, String.class);
            JSONObject jsonObject = new JSONObject(response.getBody());
            String accessToken = String.format("Bearer %s",jsonObject.get("access_token").toString());

            HttpResponse<String> accountancyServiceResponse = Unirest.post(serviceRegistryAccountancyServiceUrl)
                    .header("Content-Type", "application/json")
                    .header("accept", "*/*")
                    .header("Authorization",accessToken)
                    .body(paymentLog)
                    .asString();
            if(accountancyServiceResponse.getStatus() != 200 && accountancyServiceResponse.getStatus() != 204){
                throw new NimbleException(NimbleExceptionMessageCode.INTERNAL_SERVER_ERROR_SEND_PAYMENT_LOG.toString(), Collections.singletonList(orderId));
            }
        }
    }
}
