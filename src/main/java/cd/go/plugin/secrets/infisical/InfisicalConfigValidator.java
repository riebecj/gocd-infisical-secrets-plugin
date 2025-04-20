package cd.go.plugin.secrets.infisical;

import java.util.Map;

import com.google.gson.Gson;

import cd.go.plugin.base.validation.ValidationResult;
import cd.go.plugin.base.validation.Validator;

public class InfisicalConfigValidator implements Validator {
    private static final Gson gson = new Gson();

    @Override
    public ValidationResult validate(Map<String, String> requestBody) {
        ValidationResult result = new ValidationResult();
        InfisicalConfig config = gson.fromJson(gson.toJson(requestBody), InfisicalConfig.class);
        if (config.getInfisicalURL() == null || config.getInfisicalURL().isEmpty()) {
            result.add(InfisicalConfig.URL_KEY, "Infisical URL is required.");
        }
        if (config.getClientId() == null || config.getClientId().isEmpty()) {
            result.add(InfisicalConfig.CLIENT_ID_KEY, "Universal Auth Client ID is required.");
        }
        if (config.getClientSecret() == null || config.getClientSecret().isEmpty()) {
            result.add(InfisicalConfig.CLIENT_SECRET_KEY, "Universal Auth Client Secret is required.");
        }
        if (config.getProjectId() == null || config.getProjectId().isEmpty()) {
            result.add(InfisicalConfig.PROJECT_ID_KEY, "A Project ID is required.");
        }
        if (config.getEnvironmentSlug() == null || config.getEnvironmentSlug().isEmpty()) {
            result.add(InfisicalConfig.ENVIRONMENT_SLUG_KEY, "An Environment Slug is required.");
        }
        return result;
    }
}
