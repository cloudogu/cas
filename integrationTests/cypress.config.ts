import * as doguTestLibrary from "@cloudogu/dogu-integration-test-library";
import { defineConfig } from "cypress";
import createBundler from "@bahmutov/cypress-esbuild-preprocessor";
import * as preprocessor from "@badeball/cypress-cucumber-preprocessor";
import createEsbuildPlugin from "@badeball/cypress-cucumber-preprocessor/esbuild";

async function setupNodeEvents(
  on: Cypress.PluginEvents,
  config: Cypress.PluginConfigOptions
) {
  // Enable the Cucumber preprocessor (JSON reports, etc.)
  await preprocessor.addCucumberPreprocessorPlugin(on, config);

  on(
    "file:preprocessor",
    createBundler({
      plugins: [createEsbuildPlugin(config)],
    })
  );

  // Apply Dogu test library config customizations
  config = doguTestLibrary.configure(config);

  return config;
}

export default defineConfig({
  e2e: {
    baseUrl: "https://192.168.56.2",
    env: {
      DoguName: "cas/login",
      MaxLoginRetries: -1,
      AdminUsername: "ces-admin",
      AdminPassword: "Ecosystem2016!",
      AdminGroup: "CesAdministrators",
      ClientID: "inttest",
      ClientSecret: "integrationTestClientSecret",
      PasswordHintText: "Contact your admin",
      PrivacyPolicyURL: "https://www.triology.de/",
      TermsOfServiceURL: "https://docs.cloudogu.com/",
      ImprintURL: "https://cloudogu.com/",
    },
    specPattern: ["cypress/e2e/**/*.feature"],
    videoCompression: false,
    setupNodeEvents,
    // Plugin-specific flags (kept from JS; ignore TS complaining if your tooling is strict)
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    nonGlobalStepBaseDir: false,
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    experimentalRunAllSpecs: true,
    chromeWebSecurity: false,
  },
});
