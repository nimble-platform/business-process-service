package eu.nimble.service.bp.processor.test;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by yildiray on 3/23/2017.
 */
@Component
public class TestProcessor implements JavaDelegate {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @HystrixCommand
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        logger.info(" $$$ TestProcessor: {}", execution);
        final Map<String, Object> variables = execution.getVariables();
        for (String key: variables.keySet()) {
            logger.debug(" $$$ Variable name {}, value {}, type {}", key, variables.get(key), variables.get(key).getClass().getName());
            if(key.equals("relatedProducts")) {
                List<String> relatedProducts = (List<String>) variables.get(key);
                logger.debug(" $$$ First Related Product: {}", relatedProducts.get(0));
            }
        }
    }
}
