// Loads all commands from the dogu e2e library into this project
const doguTestLibrary = require('@cloudogu/dogu-integration-test-library')
doguTestLibrary.registerCommands()

// local commands
import "./commands/cas_commands"
import "./commands/oauth_commands"