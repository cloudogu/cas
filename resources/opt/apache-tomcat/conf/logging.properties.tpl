handlers = java.util.logging.ConsoleHandler
.handlers = java.util.logging.ConsoleHandler

############################################################
# Handler specific properties.
# Describes specific configuration info for Handlers.
############################################################

java.util.logging.ConsoleHandler.level = {{ .Env.Get "CATALINA_LOGLEVEL" }}
java.util.logging.ConsoleHandler.formatter = org.apache.juli.OneLineFormatter


############################################################
# Facility specific properties.
# Provides extra control for each logger.
############################################################


# For example, set the org.apache.catalina.util.LifecycleBase logger to log
# each component that extends LifecycleBase changing state:
org.apache.catalina.util.LifecycleBase.level = {{ .Env.Get "CATALINA_LOGLEVEL" }}
# To see debug messages in TldLocationsCache, uncomment the following line:
org.apache.jasper.compiler.TldLocationsCache.level = {{ .Env.Get "CATALINA_LOGLEVEL" }}