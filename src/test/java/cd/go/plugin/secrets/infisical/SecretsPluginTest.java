package cd.go.plugin.secrets.infisical;

import static java.util.Base64.getDecoder;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import static org.mockito.Mockito.mock;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import static cd.go.plugin.base.ResourceReader.readResource;
import static cd.go.plugin.base.ResourceReader.readResourceBytes;

public class SecretsPluginTest {
    private SecretsPlugin infisicalPlugin;
    private static final Gson GSON = new Gson();
    private static final List<String> configKeys = List.of(
        InfisicalConfig.URL_KEY,
        InfisicalConfig.CLIENT_ID_KEY,
        InfisicalConfig.CLIENT_SECRET_KEY,
        InfisicalConfig.PROJECT_ID_KEY,
        InfisicalConfig.ENVIRONMENT_SLUG_KEY
    );

    @BeforeEach
    protected void setUp() {
        infisicalPlugin = new SecretsPlugin();
        infisicalPlugin.initializeGoApplicationAccessor(mock(GoApplicationAccessor.class));
    }

    @Test
    public void shouldReturnPluginIdentifier() {
        assertThat(infisicalPlugin.pluginIdentifier()).isNotNull();
        assertThat(infisicalPlugin.pluginIdentifier().getExtension()).isEqualTo("secrets");
        assertThat(infisicalPlugin.pluginIdentifier().getSupportedExtensionVersions())
                .contains("1.0");
    }

    @Test
    void shouldReturnConfigMetadata() throws UnhandledRequestTypeException, JSONException {
        final DefaultGoPluginApiRequest request = request(PluginResponseGenerator.RequestType.GET_METADATA.toString());

        final GoPluginApiResponse expected = new PluginResponseGenerator(200, PluginResponseGenerator.RequestType.GET_METADATA)
                .withConfigMetadataProperty(InfisicalConfig.URL_KEY, true, false)
                .withConfigMetadataProperty(InfisicalConfig.CLIENT_ID_KEY, true, true)
                .withConfigMetadataProperty(InfisicalConfig.CLIENT_SECRET_KEY, true, true)
                .withConfigMetadataProperty(InfisicalConfig.PROJECT_ID_KEY, true, false)
                .withConfigMetadataProperty(InfisicalConfig.ENVIRONMENT_SLUG_KEY, true, false)
                .withConfigMetadataProperty(InfisicalConfig.SECRET_PATH_KEY, false, false);

        final GoPluginApiResponse response = infisicalPlugin.handle(request);

        assertThat(response.responseCode()).isEqualTo(200);
        assertEquals(expected.responseBody(), response.responseBody(), true);
    }

    @Test
    void shouldReturnIcon() throws UnhandledRequestTypeException {
        final DefaultGoPluginApiRequest request = request(PluginResponseGenerator.RequestType.GET_ICON.toString());

        final GoPluginApiResponse response = infisicalPlugin.handle(request);

        Map<String, String> responseBody = toMap(response.responseBody());

        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(responseBody.size()).isEqualTo(2);
        assertThat(responseBody.get("content_type")).isEqualTo(SecretsPlugin.PLUGIN_CONTENT_TYPE);
        assertThat(getDecoder().decode(responseBody.get("data"))).isEqualTo(readResourceBytes(SecretsPlugin.PLUGIN_ICON_PATH));
    }

    @Test
    void shouldReturnSecretConfigView() throws UnhandledRequestTypeException {
        final DefaultGoPluginApiRequest request = request(PluginResponseGenerator.RequestType.GET_VIEW.toString());

        final GoPluginApiResponse response = infisicalPlugin.handle(request);

        Map<String, String> responseBody = toMap(response.responseBody());

        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(responseBody.size()).isEqualTo(1);
        assertThat(responseBody.get("template")).isEqualTo(readResource(SecretsPlugin.CONFIG_VIEW_TEMPLATE_PATH));
    }

    @Test
    void shouldFailIfHasUnknownFields() throws UnhandledRequestTypeException, JSONException {
        final DefaultGoPluginApiRequest request = request(PluginResponseGenerator.RequestType.VALIDATE.toString());
        request.setRequestBody(GSON.toJson(Map.of(
            InfisicalConfig.URL_KEY, "https://example.com",
            InfisicalConfig.CLIENT_ID_KEY, "client-id",
            InfisicalConfig.CLIENT_SECRET_KEY, "client",
            InfisicalConfig.PROJECT_ID_KEY, "project-id",
            InfisicalConfig.ENVIRONMENT_SLUG_KEY, "environment-slug",
            "unknown-field", "unknown-value" // This is the unknown field that should trigger validation error
        )));
        final GoPluginApiResponse expected = new PluginResponseGenerator(200, PluginResponseGenerator.RequestType.VALIDATE)
                .withValidationUnknownField("unknown-field");

        final GoPluginApiResponse response = infisicalPlugin.handle(request);
        System.err.println(response.responseBody());
        assertThat(response.responseCode()).isEqualTo(200);
        assertEquals(expected.responseBody(), response.responseBody(), true);
    }

    @ParameterizedTest
    @FieldSource("configKeys")
    void shouldFailIfRequiredFieldsAreMissingInRequestBody(String configKey) throws UnhandledRequestTypeException, JSONException {
        final DefaultGoPluginApiRequest request = request(PluginResponseGenerator.RequestType.VALIDATE.toString());
        request.setRequestBody(GSON.toJson(configKeys.stream()
            .filter(key -> !key.equals(configKey))
            .collect(Collectors.toMap(key -> key, key -> "dummy-value")))
        );

        final GoPluginApiResponse expected = new PluginResponseGenerator(200, PluginResponseGenerator.RequestType.VALIDATE)
                .withValidationMissingField(configKey);

        final GoPluginApiResponse response = infisicalPlugin.handle(request);

        assertThat(response.responseCode()).isEqualTo(200);
        assertEquals(expected.responseBody(), response.responseBody(), true);
    }

    @Test
    void shouldPassIfRequestIsValid() throws JSONException, UnhandledRequestTypeException {
        final DefaultGoPluginApiRequest request = request(PluginResponseGenerator.RequestType.VALIDATE.toString());
        request.setRequestBody(GSON.toJson(Map.of(
            InfisicalConfig.URL_KEY, "https://example.com",
            InfisicalConfig.CLIENT_ID_KEY, "client-id",
            InfisicalConfig.CLIENT_SECRET_KEY, "client",
            InfisicalConfig.PROJECT_ID_KEY, "project-id",
            InfisicalConfig.ENVIRONMENT_SLUG_KEY, "environment-slug"
        )));

        final GoPluginApiResponse response = infisicalPlugin.handle(request);

        assertThat(response.responseCode()).isEqualTo(200);
        assertEquals("[]", response.responseBody(), true);
    }

    private Map<String, String> toMap(String response) {
        return new Gson().fromJson(response, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    private DefaultGoPluginApiRequest request(String requestName) {
        return new DefaultGoPluginApiRequest("secrets", "1.0", requestName);
    }
}
