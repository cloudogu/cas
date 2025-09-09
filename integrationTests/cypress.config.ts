import doguTestLibrary from "@cloudogu/dogu-integration-test-library";
import { defineConfig } from "cypress";
// @ts-ignore
import fsConf from "cypress-fs/plugins/index.js";
// @ts-ignore
import createBundler from "@bahmutov/cypress-esbuild-preprocessor";
import preprocessor from "@badeball/cypress-cucumber-preprocessor";
import createEsbuildPlugin from "@badeball/cypress-cucumber-preprocessor/esbuild";

async function setupNodeEvents(on: Cypress.PluginEvents, config: Cypress.ConfigOptions): Promise<Cypress.ConfigOptions> {
  await preprocessor.addCucumberPreprocessorPlugin(on, config);
  on(
    "file:preprocessor",
    createBundler({
      plugins: [createEsbuildPlugin.default(config)],
    })
  );
  fsConf(on);
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
    } as Record<string, string | number>,

    specPattern: ["cypress/e2e/**/*.feature"],
    videoCompression: false,
    setupNodeEvents,
    nonGlobalStepBaseDir: false,
    chromeWebSecurity: false,
    experimentalRunAllSpecs: true,
  },
});