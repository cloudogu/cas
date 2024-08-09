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

  return config;
}

module.exports = defineConfig({
    e2e: {
        "baseUrl": "https://192.168.56.2",
        "env": {
            "DoguName": "cas/login",
            "MaxLoginRetries": -1,
            "AdminUsername": "admin",
            "AdminPassword": "Asdf$1",
            "AdminGroup": "admin",
            "ClientID": "inttest",
            "ClientSecret": "integrationTestClientSecret",
            "PasswordHintText": "Contact your admin",
            "PrivacyPolicyURL": "https://www.triology.de/",
            "TermsOfServiceURL": "https://docs.cloudogu.com/",
            "ImprintURL": "https://cloudogu.com/"
        },

        specPattern: ["cypress/e2e/**/*.feature"],
        videoCompression: false,
        setupNodeEvents,
        nonGlobalStepBaseDir: false,
        chromeWebSecurity: false,
        experimentalRunAllSpecs: true,
    }
})
