package com.axway.apim.test.envProperties;

import com.axway.apim.APIManagerMockBase;
import com.axway.apim.api.API;
import com.axway.apim.apiimport.APIImportConfigAdapter;
import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.EnvironmentProperties;
import com.axway.apim.lib.utils.TestIndicator;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Properties;

public class SubstituteVariablesTest extends APIManagerMockBase {

    private static String OS = null;

    @BeforeClass
    private void initCommandParameters() throws IOException {
        setupMockData();
        TestIndicator.getInstance().setTestRunning(true);
    }

    @Test
    public void validateSystemOSAreSubstituted() throws IOException {
        String configFile = "com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
        String pathToConfigFile = this.getClass().getClassLoader().getResource(configFile).getFile();
        String apiDefinition = "/api_definition_1/petstore.json";

        APIImportConfigAdapter importConfig = new APIImportConfigAdapter(pathToConfigFile, null, apiDefinition, null);

        API testAPI = importConfig.getApiConfig();
        if (System.getenv("TRAVIS") != null && System.getenv("TRAVIS").equals("true")) {
            // At Travis an environment-variable USER exists which should have been replaced
            Assert.assertNotEquals(testAPI.getName(), "${USER}");
        } else if (System.getenv("GITHUB_ACTIONS") != null && System.getenv("GITHUB_ACTIONS").equals("true")) {
            // When running at GitHub actions
            Assert.assertNotEquals(testAPI.getVhost(), "${GITHUB_ACTION}");
        } else {
            // On Windows use USERNAME in the version
            if (isWindows()) {
                Assert.assertNotEquals(testAPI.getVersion(), "${USERNAME}");
            } else { // MacOS X and Linux use USER in the name
                Assert.assertNotEquals(testAPI.getName(), "${USER}");
            }
        }
    }

    @Test
    public void validateBaseEnvReplacedOSAttribute() throws IOException {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "apimcli";
        Properties props = System.getProperties();
        props.setProperty("OS_AND_MAIN_ENV_PROPERTY", "valueFromOS");

        EnvironmentProperties envProps = new EnvironmentProperties(null, path);
        CoreParameters.getInstance().setProperties(envProps);

        String configFile = "com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
        String pathToConfigFile = this.getClass().getClassLoader().getResource(configFile).getFile();
        String apiDefinition = "/api_definition_1/petstore.json";

        APIImportConfigAdapter importConfig = new APIImportConfigAdapter(pathToConfigFile, null, apiDefinition, null);

        API testAPI = importConfig.getApiConfig();

        Assert.assertEquals(testAPI.getPath(), "valueFromMainEnv");
    }

    @Test
    public void validateStageEnvOveridesAll() throws IOException {
        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "apimcli";
        Properties props = System.getProperties();
        props.setProperty("OS_MAIN_AND_STAGE_ENV_PROPERTY", "valueFromOS");

        EnvironmentProperties envProps = new EnvironmentProperties("anyOtherStage", path);
        CoreParameters.getInstance().setProperties(envProps);

        String configFile = "com/axway/apim/test/files/envProperties/1_config-with-os-variable.json";
        String pathToConfigFile = this.getClass().getClassLoader().getResource(configFile).getFile();
        String apiDefinition = "/api_definition_1/petstore.json";

        APIImportConfigAdapter importConfig = new APIImportConfigAdapter(pathToConfigFile, null, apiDefinition, null);

        API testAPI = importConfig.getApiConfig();

        Assert.assertEquals(testAPI.getDescriptionManual(), "valueFromAnyOtherStageEnv");
    }

    private static String getOsName() {
        if (OS == null) {
            OS = System.getProperty("os.name");
        }
        return OS;
    }

    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }
}
