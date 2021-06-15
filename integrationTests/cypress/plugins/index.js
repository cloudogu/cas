const doguTestLibrary = require('@cloudogu/dogu-integration-test-library')
const cucumber = require('cypress-cucumber-preprocessor').default

/**
 * @type {Cypress.PluginConfig}
 */
module.exports = (on, config) => {
    config = doguTestLibrary.configure(config)
    on('file:preprocessor', cucumber())
    return config
}
