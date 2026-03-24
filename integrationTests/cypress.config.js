const doguTestLibrary = require("@cloudogu/dogu-integration-test-library");
const { defineConfig } = require("cypress");
const createBundler = require("@bahmutov/cypress-esbuild-preprocessor");
const preprocessor = require("@badeball/cypress-cucumber-preprocessor");
const createEsbuildPlugin = require("@badeball/cypress-cucumber-preprocessor/esbuild");

async function setupNodeEvents(on, config) {
  // This is required for the preprocessor to be able to generate JSON reports after each run, and more,
  await preprocessor.addCucumberPreprocessorPlugin(on, config);
  on(
    "file:preprocessor",
    createBundler({
      plugins: [createEsbuildPlugin.default(config)],
    })
  );

  config = doguTestLibrary.configure(config);

  if (!config.env.TAGS) {
    // "team-ces" is from https://github.com/cloudogu/dogu-build-lib/blob/0f2b2b2b8ff6be4ffdfd61c660008e575d721195/src/com/cloudogu/ces/dogubuildlib/MultiNodeEcoSystem.groovy#L265
    config.env.TAGS = config.env.AdminUsername == "team-ces"
        ? "not @classic"
        : "not @multinode";
  } else {
    config.env.TAGS += config.env.AdminUsername == "team-ces"
        ? " and not @classic"
        : " and not @multinode";
  }
  config.env.TAGS += " and not @disabled"

  return config;
}

module.exports = defineConfig({
  e2e: {
    baseUrl: "https://192.168.56.2",
    env: {
      DoguName: "cas/login",
      MaxLoginRetries: -1,
      "AdminUsername": "ces-admin",
      "AdminPassword": "Ecosystem2016!",
      "AdminGroup": "CesAdministrators",
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
    nonGlobalStepBaseDir: false,
    chromeWebSecurity: false,
    experimentalRunAllSpecs: true,
  },
});