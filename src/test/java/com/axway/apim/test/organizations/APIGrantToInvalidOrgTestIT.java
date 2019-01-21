package com.axway.apim.test.organizations;

import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.axway.apim.test.SwaggerImportTestAction;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;
import com.consol.citrus.functions.core.RandomNumberFunction;

@Test(testName="APIGrantToInvalidOrgTestIT")
public class APIGrantToInvalidOrgTestIT extends TestNGCitrusTestDesigner {
	
	@Autowired
	private SwaggerImportTestAction swaggerImport;
	
	@CitrusTest(name = "APIGrantToInvalidOrgTestIT")
	public void setupDevOrgTest() {
		description("Tool must fail with a defined error, if a configured org is invalid");
		
		variable("apiNumber", RandomNumberFunction.getRandomNumber(3, true));
		variable("apiPath", "/grant_invalid_org-api-${apiNumber}");
		variable("apiName", "Grant to invalid orgs API-${apiNumber}");

		
		echo("####### Importing API: '${apiName}' on path: '${apiPath}' for the first time #######");
		
		createVariable("swaggerFile", "/com/axway/apim/test/files/basic/petstore.json");
		createVariable("configFile", "/com/axway/apim/test/files/organizations/1_api-with-client-orgs.json");
		createVariable("state", "published");
		createVariable("orgName", "${orgName}");
		createVariable("orgName2", "Invalid Org 0815");
		createVariable("expectedReturnCode", "57");
		action(swaggerImport);
	}
}
