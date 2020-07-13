package eu.nimble.service.bp.util.eFactory;

import com.google.common.base.Strings;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AccountancyService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${efactory.service-registry.accountancy-service.url}")
    private String serviceRegistryAccountancyServiceUrl;

    private String accountancyServiceLogstashEndpoint;

    @PostConstruct
    public void init(){
        if(!Strings.isNullOrEmpty(serviceRegistryAccountancyServiceUrl)){
            try {
                HttpResponse<JsonNode> response = Unirest.get(serviceRegistryAccountancyServiceUrl)
                        .header("accept", "*/*")
                        .asJson();
                JSONArray apis = (JSONArray) response.getBody().getObject().get("apis");
                int apiSize = apis.length();
                for(int i = 0 ; i < apiSize ; i++){
                    JSONObject jsonObject = (JSONObject) apis.get(i);
                    if(jsonObject.get("id").toString().contentEquals("logstash-endpoint")){
                        accountancyServiceLogstashEndpoint = jsonObject.get("endpoint").toString();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve service registry information for accountancy service",e);
            }
        }
    }

    public String getAccountancyServiceLogstashEndpoint() {
        return accountancyServiceLogstashEndpoint;
    }
}
