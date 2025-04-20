package cd.go.plugin.secrets.infisical;

import java.util.ArrayList;
import java.util.Map;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

public class PluginResponseGenerator extends GoPluginApiResponse {
    private final Gson gson = new Gson();
    private final int responseCode;
    private final String requestType;
    private final ArrayList<Map<String, Object>> metadata = new ArrayList<>();
    private final ArrayList<Map<String, Object>> validation = new ArrayList<>();
    
    public PluginResponseGenerator(int responseCode, RequestType requestType) {
        this.responseCode = responseCode;
        this.requestType = requestType.toString();
    }

    public PluginResponseGenerator withConfigMetadataProperty(String key, Boolean required, Boolean secure) {
        this.metadata.add(Map.of(
                "key", key,
                "metadata", Map.of(
                        "required", required,
                        "secure", secure,
                        "display_name", ""
                )
        ));
        return this;
    }

    public PluginResponseGenerator withValidationUnknownField(String key) {
        this.validation.add(Map.of(
                "key", key,
                "message", "Is an unknown property"
        ));
        return this;
    }

    public PluginResponseGenerator withValidationMissingField(String key) {
        this.validation.add(Map.of(
                "key", key,
                "message", key + " must not be blank."
        ));
        return this;
    }

    public enum RequestType {
        GET_ICON("go.cd.secrets.get-icon"),
        GET_METADATA("go.cd.secrets.secrets-config.get-metadata"),
        GET_VIEW("go.cd.secrets.secrets-config.get-view"),
        VALIDATE("go.cd.secrets.secrets-config.validate"),
        LOOKUP("go.cd.secrets.secrets-lookup");

        public final String requestType;

        private RequestType(String requestType) {
            this.requestType = requestType;
        }

        @Override
        public String toString() {
            return this.requestType;
        }
    }

    @Override
    public int responseCode() {
        return responseCode;
    }

    @Override
    public Map<String, String> responseHeaders() {
        return Map.of("Content-Type", "application/json");
    }

    @Override
    public String responseBody() {
        if (this.requestType.equals(RequestType.GET_METADATA.toString())) {
            return gson.toJson(this.metadata);
        } else if (this.requestType.equals(RequestType.VALIDATE.toString())) {
            return gson.toJson(this.validation);
        } else {
            return "";
        }
    }
}
