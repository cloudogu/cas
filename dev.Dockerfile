# registry.cloudogu.com/official/cas
FROM registry.cloudogu.com/official/java:11.0.5-4

LABEL NAME="official/cas" \
      VERSION="6.3.3-1" \
      maintainer="hello@cloudogu.com"

# configure environment
ENV TOMCAT_MAJOR_VERSION=9 \
	TOMCAT_VERSION=9.0.41 \
	TOMCAT_TARGZ_SHA256=6a5fc1f79f002f25480e3a50daa1fb16fdb2f0a969bc2f806c88bc550002cf71 \
	CATALINA_BASE=/opt/apache-tomcat \
	CATALINA_PID=/var/run/tomcat7.pid \
	CATALINA_SH=/opt/apache-tomcat/bin/catalina.sh \
	SERVICE_TAGS=webapp \
	USER=cas \
    GROUP=cas

# run installation
RUN set -x \
 # create group and user for cas
 && addgroup -S -g 1000 ${GROUP} \
 && adduser -S -h "/var/lib/${USER}" -s /bin/bash -G ${GROUP} -u 1000 ${USER} \
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
 && mkdir -p /etc/cas/config \
 && mkdir -p /etc/cas/saml

# copy overlay
COPY --chown=${USER}:${GROUP} app/build/libs/cas.war ${CATALINA_BASE}/webapps/cas/cas.war

RUN set -x \
 && cd ${CATALINA_BASE}/webapps/cas/ \
 && unzip cas.war \
 && rm -f cas.war \
 && chown -R ${USER}:${GROUP} ${CATALINA_BASE}

# copy resources
COPY --chown=${USER}:${GROUP} resources /

RUN chown -R ${USER}:${GROUP} /etc/cas

# expose tomcat port
EXPOSE 8080

HEALTHCHECK CMD doguctl healthy cas || exit 1

CMD /startup.sh
