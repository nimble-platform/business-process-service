package eu.nimble.service.bp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Value("${nimble.platformHost}")
    private String platformHost;

    @Bean
    public Docket api() {

        platformHost = platformHost.replace("https://", "");
        platformHost = platformHost.replace("http://","");

        return new Docket(DocumentationType.SWAGGER_2)
                .host(platformHost)
                .select()
                .apis(RequestHandlerSelectors.basePackage("eu.nimble"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(metaData());
    }

    private ApiInfo metaData(){
        return new ApiInfo(
                "NIMBLE Business Process REST API",
                "REST API handling process instances on the NIMBLE platform",
                "1.0",
                "",
                "",
                "Additional documentation related to Business Process REST API",
                "https://secure.salzburgresearch.at/wiki/display/NIMBLE/Task+2.3+Design+of+an+Open+API+for+the+NIMBLE+Platform"
        );
    }
}
