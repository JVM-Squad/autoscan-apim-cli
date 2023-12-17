package com.axway.apim.test.security;

import com.axway.apim.EndpointConfig;
import com.axway.apim.test.ImportTestAction;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.functions.core.RandomNumberFunction;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.message.MessageType;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import static org.citrusframework.DefaultTestActionBuilder.action;
import static org.citrusframework.actions.EchoAction.Builder.echo;
import static org.citrusframework.dsl.JsonPathSupport.jsonPath;
import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.citrusframework.validation.DelegatingPayloadVariableExtractor.Builder.fromBody;

@ContextConfiguration(classes = {EndpointConfig.class})
public class UnpublishedApiKeyTestIT extends TestNGCitrusSpringSupport {

    @Autowired
    HttpClient apiManager;

    @CitrusTest(name = "UnpublishedApiKeyTest")
    @Test
    public void run() {
        ImportTestAction swaggerImport = new ImportTestAction();
        description("Some checks for the API-Key security device");

        variable("apiNumber", RandomNumberFunction.getRandomNumber(3, true));
        variable("apiPath", "/api-key-test-${apiNumber}");
        variable("apiName", "API Key Test ${apiNumber}");
        variable("status", "unpublished");

        $(echo("####### Importing API: '${apiName}' on path: '${apiPath}' with following settings: #######"));
        variable("apiKeyFieldName", "KeyId");
        variable("takeFrom", "HEADER");
        variable("removeCredentialsOnSuccess", "false");
        variable(ImportTestAction.API_DEFINITION, "/com/axway/apim/test/files/security/petstore.json");
        variable(ImportTestAction.API_CONFIG, "/com/axway/apim/test/files/security/1_api-apikey.json");
        variable("expectedReturnCode", "0");
        $(action(swaggerImport));

        $(echo("####### Simulate Re-Import without changes #######"));
        variable("apiKeyFieldName", "KeyId");
        variable("takeFrom", "HEADER");
        variable("removeCredentialsOnSuccess", "false");
        variable(ImportTestAction.API_DEFINITION, "/com/axway/apim/test/files/security/petstore.json");
        variable(ImportTestAction.API_CONFIG, "/com/axway/apim/test/files/security/1_api-apikey.json");
        variable("expectedReturnCode", "10");
        $(action(swaggerImport));

        $(echo("####### Validate API: '${apiName}' on path: '${apiPath}' with correct settings #######"));
        $(http().client(apiManager).send().get("/proxies").name("api"));
        $(http().client(apiManager).receive().response(HttpStatus.OK).message().type(MessageType.JSON).validate(jsonPath()
            .expression("$.[?(@.path=='${apiPath}')].name", "${apiName}")
            .expression("$.[?(@.path=='${apiPath}')].state", "unpublished")
            .expression("$.[?(@.path=='${apiPath}')].securityProfiles[*].name", "@assertThat(hasSize(1))@") // Only one security profile is expected!
            .expression("$.[?(@.path=='${apiPath}')].securityProfiles[0].devices[0].type", "apiKey")
            .expression("$.[?(@.path=='${apiPath}')].securityProfiles[0].devices[0].properties.takeFrom", "${takeFrom}")
            .expression("$.[?(@.path=='${apiPath}')].securityProfiles[0].devices[0].properties.apiKeyFieldName", "${apiKeyFieldName}")
            .expression("$.[?(@.path=='${apiPath}')].securityProfiles[0].devices[0].properties.removeCredentialsOnSuccess", "${removeCredentialsOnSuccess}")).extract(fromBody()
            .expression("$.[?(@.path=='${apiPath}')].id", "apiId")));

        $(echo("####### Change the API-Security settings (as it's unpublished, API-ID must stay the same) #######"));
        variable("apiKeyFieldName", "KeyId-Test");
        variable("takeFrom", "QUERY");
        variable("removeCredentialsOnSuccess", "true");
        variable(ImportTestAction.API_DEFINITION, "/com/axway/apim/test/files/security/petstore.json");
        variable(ImportTestAction.API_CONFIG, "/com/axway/apim/test/files/security/1_api-apikey.json");
        variable("expectedReturnCode", "0");
        $(action(swaggerImport));

        $(echo("####### Validate the Security-Settings have been changed (without changing the API-ID) #######"));
        $(http().client(apiManager).send().get("/proxies/${apiId}"));
        $(http().client(apiManager).receive().response(HttpStatus.OK).message().type(MessageType.JSON).validate(jsonPath()
            .expression("$.[?(@.id=='${apiId}')].id", "${apiId}")
            .expression("$.[?(@.id=='${apiId}')].securityProfiles[0].devices[0].properties.takeFrom", "${takeFrom}")
            .expression("$.[?(@.id=='${apiId}')].securityProfiles[0].devices[0].properties.apiKeyFieldName", "${apiKeyFieldName}")
            .expression("$.[?(@.id=='${apiId}')].securityProfiles[0].devices[0].properties.removeCredentialsOnSuccess", "${removeCredentialsOnSuccess}")
            .expression("$.[?(@.id=='${apiId}')].state", "unpublished")));
    }

}
