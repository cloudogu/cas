#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

CAS_PROPERTIES_TEMPLATE="/opt/apache-tomcat/cas.properties.conf.tpl"
CAS_PROPERTIES="/opt/apache-tomcat/webapps/cas/WEB-INF/cas.properties"

echo "Getting general variables for templates..."
DOMAIN=$(doguctl config --global domain)
FQDN=$(doguctl config --global fqdn)

echo "Rendering templates..."
sed "s@%DOMAIN%@$DOMAIN@g;\
s@%FQDN%@$FQDN@g"\
 /opt/apache-tomcat/cas.properties.tpl >> /etc/cas/config/cas.properties

cp -r /opt/apache-tomcat/services /opt/apache-tomcat/webapps/cas/WEB-INF/services

echo "Creating truststore, which is used in the setenv.sh..."
create_truststore.sh > /dev/null

doguctl state ready

echo "Starting cas..."
exec su - cas -c "${CATALINA_SH} run"