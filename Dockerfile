FROM maven:3.6.0-jdk-8 as builder
COPY /app/pom.xml /cas/pom.xml
RUN set -x \
     && cd /cas \
     && mvn dependency:resolve
COPY /app/ /cas
RUN set -x \
    && cd /cas \
    && mvn package

# registry.cloudogu.com/official/cas
FROM registry.cloudogu.com/official/java:8u252-1

LABEL NAME="official/cas" \
    VERSION="4.0.7.20-12" \
    maintainer="michael.behlendorf@cloudogu.com"

# configure environment
ENV TOMCAT_MAJOR_VERSION=8 \
	TOMCAT_VERSION=8.0.53 \
	CATALINA_BASE=/opt/apache-tomcat \
	CATALINA_PID=/var/run/tomcat7.pid \
	CATALINA_SH=/opt/apache-tomcat/bin/catalina.sh \
	SERVICE_TAGS=webapp \
	TOMCAT_TARGZ_SHA256=19a047c4425c4ea796215d397b7caeda958c764981624ea5c4f763d98d2db7fa

COPY --from=builder /cas/target/cas.war /cas.war

# run installation
RUN set -x \
 # create group and user for cas
 && addgroup -S -g 1000 cas \
 && adduser -S -h /var/lib/cas -s /bin/bash -G cas -u 1000 cas \
 # install tomcat
 && mkdir -p /opt \
 && wget --progress=bar:force:noscroll "http://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_MAJOR_VERSION}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && echo "${TOMCAT_TARGZ_SHA256} *apache-tomcat-${TOMCAT_VERSION}.tar.gz" | sha256sum -c - \
 && tar -C /opt -xzvf "apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && rm -f "apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && mv /opt/apache-tomcat-* ${CATALINA_BASE} \
 && rm -rf ${CATALINA_BASE}/webapps/* \
 # install cas webapp application
 && mkdir ${CATALINA_BASE}/webapps/cas/ \
 && mv /cas.war ${CATALINA_BASE}/webapps/cas/cas.war \
 && cd ${CATALINA_BASE}/webapps/cas/ \
 && unzip cas.war \
 && rm -f cas.war \
 && chown -R cas:cas ${CATALINA_BASE}

# copy resources
COPY resources /

# expose tomcat port
EXPOSE 8080

HEALTHCHECK CMD doguctl healthy cas || exit 1

# start tomcat as user tomcat
CMD /startup.sh

