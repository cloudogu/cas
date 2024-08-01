#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

## This is a dev file to improve local development. Remove when feature #201 is done

docker exec -it cas bash -c "rm -rf /opt/apache-tomcat/webapps/cas/WEB-INF/classes/templates"
docker exec -it cas bash -c "rm -rf /opt/apache-tomcat/webapps/cas/WEB-INF/classes/static/css"
docker exec -it cas bash -c "rm -rf /opt/apache-tomcat/webapps/cas/WEB-INF/classes/static/js"
docker exec -it cas bash -c "rm -rf /opt/apache-tomcat/webapps/cas/WEB-INF/classes/static/images"
docker cp app/src/main/resources/templates cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes
docker cp app/src/main/resources/cas-theme-default.properties cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes
docker cp app/src/main/resources/static/css cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes/static/css
docker cp app/src/main/resources/static/js cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes/static/js
docker cp app/src/main/resources/static/images cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes/static/images
docker cp app/src/main/resources/custom_messages_de.properties cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes/custom_messages_de.properties
docker cp app/src/main/resources/custom_messages.properties cas:/opt/apache-tomcat/webapps/cas/WEB-INF/classes/custom_messages.properties
docker kill cas
docker start cas