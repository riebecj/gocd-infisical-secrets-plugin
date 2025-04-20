package cd.go.plugin.secrets.infisical;

import static java.util.Collections.singletonList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.infisical.sdk.InfisicalSdk;
import com.infisical.sdk.config.SdkConfig;
import com.infisical.sdk.models.Secret;
import com.infisical.sdk.util.InfisicalException;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import cd.go.plugin.base.GsonTransformer;
import cd.go.plugin.base.annotations.Property;
import cd.go.plugin.base.dispatcher.BaseBuilder;
import cd.go.plugin.base.dispatcher.RequestDispatcher;
import cd.go.plugin.base.executors.secrets.LookupExecutor;

@Extension
public class SecretsPlugin implements GoPlugin {
    private RequestDispatcher requestDispatcher;

    public static final String PLUGIN_ICON_PATH = "/plugin-icon.png";
    public static final String PLUGIN_CONTENT_TYPE = "image/png";
    public static final String CONFIG_VIEW_TEMPLATE_PATH = "/secrets.template.html";

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        requestDispatcher = BaseBuilder.forSecrets()
                .v1()
                .icon(PLUGIN_ICON_PATH, PLUGIN_CONTENT_TYPE)  // Icon file path and content type
                .configMetadata(InfisicalConfig.class)     // Secret config class
                .configView(CONFIG_VIEW_TEMPLATE_PATH)   // Angular html template for the secret config view
                .validateSecretConfig()  // You can add additional validators to validate your secret configs
                .lookup(new SecretConfigLookupExecutor()) // lookup executor
                .build();
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        try {
            return requestDispatcher.dispatch(request); // use previously built request dispatcher to handle server requests
        } catch (UnhandledRequestTypeException e) {
            //Handle it
            throw new RuntimeException(e);
        }
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("secrets", singletonList("1.0"));
    }
}

class InfisicalConfig {
    public static final String URL_KEY = "InfisicalURL";
    public static final String CLIENT_ID_KEY = "ClientId";
    public static final String CLIENT_SECRET_KEY = "ClientSecret";
    public static final String PROJECT_ID_KEY = "ProjectId";
    public static final String ENVIRONMENT_SLUG_KEY = "EnvironmentSlug";
    public static final String SECRET_PATH_KEY = "SecretPath";

    @Expose
    @SerializedName(URL_KEY)
    @Property(name = URL_KEY, required = true)
    private String infisicalURL;

    @Expose
    @SerializedName(CLIENT_ID_KEY)
    @Property(name = CLIENT_ID_KEY, required = true, secure = true)
    private String clientId;

    @Expose
    @SerializedName(CLIENT_SECRET_KEY)
    @Property(name = CLIENT_SECRET_KEY, required = true, secure = true)
    private String clientSecret;

    @Expose
    @SerializedName(PROJECT_ID_KEY)
    @Property(name = PROJECT_ID_KEY, required = true)
    private String projectId;

    @Expose
    @SerializedName(ENVIRONMENT_SLUG_KEY)
    @Property(name = ENVIRONMENT_SLUG_KEY, required = true)
    private String environmentSlug;

    @Expose
    @SerializedName(SECRET_PATH_KEY)
    @Property(name = SECRET_PATH_KEY, required = false)
    private String secretPath;

    public String getInfisicalURL() {
        return infisicalURL;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getEnvironmentSlug() {
        return environmentSlug;
    }

    public String getSecretPath() {
        if (secretPath == null || secretPath.isEmpty()) {
            return "/"; // Default to root path if not specified
        }
        return secretPath;
    }
}

class SecretsLookupRequest {
    @Expose
    @SerializedName("configuration")
    private InfisicalConfig secretConfig;

    @Expose
    @SerializedName("keys")
    private List<String> keys;

    public InfisicalConfig getInfisicalConfig() {
        return secretConfig;
    }

    public List<String> getKeys() {
        return keys;
    }
}

class SecretConfigLookupExecutor extends LookupExecutor<SecretsLookupRequest> {

    @Override
    protected GoPluginApiResponse execute(SecretsLookupRequest request) {
        InfisicalConfig config = request.getInfisicalConfig();
        InfisicalSdk infisical = new InfisicalSdk(new SdkConfig.Builder()
            .withSiteUrl(config.getInfisicalURL())
            .build()
        );
        try {
            infisical.Auth().UniversalAuthLogin(config.getClientId(), config.getClientSecret());
        } catch (InfisicalException e) {
            return DefaultGoPluginApiResponse.error("Failed to authenticate with Infisical: " + e.getMessage());
        }
        try {
            List<Secret> secretsList = infisical.Secrets().ListSecrets(config.getProjectId(), config.getEnvironmentSlug(), config.getSecretPath(), true, true, true);
            List<Map<String,String>> secrets = secretsList.stream()
                .filter(secret -> request.getKeys().contains(secret.getSecretKey()))
                .map((secret) -> Map.of("key", secret.getSecretKey(), "value", secret.getSecretValue()))
                .collect(Collectors.toList());
            return DefaultGoPluginApiResponse.success(new Gson().toJson(secrets));
        } catch (InfisicalException e) {
            return DefaultGoPluginApiResponse.error("Failed to fetch secrets from Infisical: " + e.getMessage());
        }
    }

    @Override
    protected SecretsLookupRequest parseRequest(String body) {
        return GsonTransformer.fromJson(body, SecretsLookupRequest.class);
    }
}
