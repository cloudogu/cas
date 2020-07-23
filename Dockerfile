# registry.cloudogu.com/official/cas
FROM registry.cloudogu.com/official/java:11.0.5-2

LABEL NAME="official/cas" \
    VERSION="6.2.0.1-1" \
    maintainer="michael.behlendorf@cloudogu.com"

# configure environment
ENV TOMCAT_MAJOR_VERSION=8 \
	TOMCAT_VERSION=8.5.57 \
	CATALINA_BASE=/opt/apache-tomcat \
	CATALINA_PID=/var/run/tomcat7.pid \
	CATALINA_SH=/opt/apache-tomcat/bin/catalina.sh \
	SERVICE_TAGS=webapp \
	TOMCAT_TARGZ_SHA512=720de36bb3e40a4c67bdf0137b12ae0fd733aef772d81a4b8dab00f29924ddd17ecb2a7217b9551fc0ca51bd81d1da13ad63b6694c445e5c0e42dfa7f279ede1

# run installation
RUN set -x \
 # create group and user for cas
 && addgroup -S -g 1000 cas \
 && adduser -S -h /var/lib/cas -s /bin/bash -G cas -u 1000 cas \
 # install tomcat
 && mkdir -p /opt \
 && wget --progress=bar:force:noscroll "http://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_MAJOR_VERSION}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && echo "${TOMCAT_TARGZ_SHA512} *apache-tomcat-${TOMCAT_VERSION}.tar.gz" | sha512sum -c - \
 && tar -C /opt -xzvf "apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && rm -f "apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && mv /opt/apache-tomcat-* ${CATALINA_BASE} \
 && rm -rf ${CATALINA_BASE}/webapps/* \
 # install cas webapp application
 && mkdir ${CATALINA_BASE}/webapps/cas/ \
 && mkdir -p /etc/cas/config \
 && mkdir -p /etc/cas/saml

COPY app6/build/libs/cas.war /cas.war
RUN set -x \
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

