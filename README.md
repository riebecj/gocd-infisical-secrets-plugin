# Infisical Secrets Plugin for GoCD

This is a GoCD Secrets Plugin which allows users to use [Infisical](https://infisical.com/docs/documentation/getting-started/introduction) as a secrets manager fo GoCD.


## Table of Contents
* [Requirements](#requirements)
* [Installation](#installation)
  * [Configure](#configure-the-plugin-to-access-secrets-from-aws)
  * [Usage](https://docs.gocd.org/current/configuration/secrets_management.html#step-4-define-secret-params)
* [Building the code base](#building-the-code-base)
* [Troubleshooting](#troubleshooting)
  * [Debug Logs](#enable-debug-logs)


# Requirements

* GoCD server version `v19.6.0` or above
* An Infisical [Machine Identity](https://infisical.com/docs/documentation/platform/identities/machine-identities) with the correct permissions to your desired project and a valid [Universal Auth](https://infisical.com/docs/documentation/platform/identities/universal-auth) **Client ID** and **Client Secret**.

> The plugin needs to be configured with a Secret Config in order to connect with Infisical. Configure it after installing the plugin.

# Installation

- Download the [latest release](https://github.com/riebecj/gocd-infisical-secrets-plugin/releases) to your GoCd server's `${GO_SERVER_DIR}/plugins/external` directory.

    **Alternatively:** *[Build from source](#build-from-source) and then copy the file `build/libs/gocd-infisical-secrets-plugin-$VERSION.jar` to the GoCD server's external plugins directory.*

- Restart the server.

> The `GO_SERVER_DIR` is usually `/var/lib/go-server` on **Linux** and `C:\Program Files\Go Server` on **Windows**.

## Configure the plugin to access secrets from AWS

- Login to your GoCD server.
- Navigate to **Admin** > **Secret Management**.
- Click on **ADD** button and select `GoCD secrets plugin for Infisical`.
- Configure the mandatory fields.

    | Field           | Required  | Description                                                         |
    | --------------- | --------- | --------------------------------------------------------------------|
    | InfisicalURL    | true      | The Infisical URL (i.e. `https://infisical.example.com`)              |
    | ClientId        | true      | The Universal Auth Client ID of your Machine Identity.              |
    | ClientSecret    | true      | The Universal Auth Client Secret of your Machine Identity.          |
    | ProjectId       | true      | The Project ID in Infisical where you want to access secrets.       |
    | EnvironmentSlug | true      | The Slug of the Environment where you want to pull secrets from.    |
    | SecretPath      | false     | The folder path to pull secrets from. Defaults to `/`.              |

    **NOTE:** *The plugin is pre-configured to expand [Secret References](https://infisical.com/docs/documentation/platform/secret-reference#secret-referencing), include [Secret Imports](https://infisical.com/docs/documentation/platform/secret-reference#secret-imports), and recursively search the project folder for secrets. To limit access within the project/environment pair, provide a specific `SecretPath` in your config (i.e. `/myTeam/myFolder`)*

- Configure the `rules` where this secrets can be used.
`<rules>` tag defines where this secretConfig is allowed/denied to be referred. For more details about rules and examples refer the GoCD Secret Management [documentation](https://docs.gocd.org/current/configuration/secrets_management.html#step-3-restrict-usage-of-secrets-manager)

- Save.

# Build from source
To build the jar, run `./gradlew clean test assemble`

# Troubleshooting

## Enable Debug Logs

### If you are on GoCD version 19.6 and above:

Edit the file `wrapper-properties.conf` on your GoCD server and add the following options. The location of the `wrapper-properties.conf` can be found in the [installation documentation](https://docs.gocd.org/current/installation/installing_go_server.html) of the GoCD server.

```properties
# We recommend that you begin with the index `100` and increment the index for each system property
wrapper.java.additional.100=-Dplugin-cd.go.plugin.secrets.infisical.log.level=debug
```

### GoCD server 19.6 and above on docker using one of the supported GoCD server images:

set the environment variable `GOCD_SERVER_JVM_OPTIONS`:

```shell
docker run -e "GOCD_SERVER_JVM_OPTIONS=-Dplugin-cd.go.plugin.secrets.infisical.log.level=debug" ...
```

The plugin logs are written to `LOG_DIR/plugin-cd.go.plugin.secrets.infisical.log`. The log dir 
- on Linux is `/var/log/go-server`
- on Windows are written to `C:\Program Files\Go Server\logs` 
- on docker images are written to `/godata/logs`