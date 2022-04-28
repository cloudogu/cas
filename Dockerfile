FROM adoptopenjdk/openjdk11:alpine-slim AS builder

RUN mkdir -p /cas-overlay
COPY ./app/gradle/ /cas-overlay/gradle/
COPY ./app/gradlew ./app/settings.gradle ./app/build.gradle ./app/gradle.properties /cas-overlay/
WORKDIR /cas-overlay

# Cache gradle
RUN chmod 750 ./gradlew \
    && ./gradlew --version

# Cache dependencies
RUN ./gradlew clean build --parallel --no-daemon

# Copy source code and build overlay
COPY ./app/src /cas-overlay/src/
RUN ./gradlew clean build --parallel --no-daemon

# registry.cloudogu.com/official/cas
FROM registry.cloudogu.com/official/java:11.0.14-3

LABEL NAME="official/cas" \
      VERSION="6.5.3-4" \
      maintainer="hello@cloudogu.com"

# update packages of the image
RUN set -o errexit \
  && set -o nounset \
  && set -o pipefail \
  && apk update \
  && apk upgrade

# configure environment
ENV TOMCAT_MAJOR_VERSION=9 \
	TOMCAT_VERSION=9.0.62 \
	TOMCAT_TARGZ_SHA256=03157728a832cf9c83048cdc28d09600cbb3e4fa087f8b97d74c8b4f34cd89bb \
	CATALINA_BASE=/opt/apache-tomcat \
	CATALINA_PID=/var/run/tomcat7.pid \
	CATALINA_SH=/opt/apache-tomcat/bin/catalina.sh \
	SERVICE_TAGS=webapp \
	USER=cas \
    GROUP=cas \
    SSL_BASE_DIRECTORY="/etc/ssl"

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
 && mkdir -p /etc/cas/saml \
 && mkdir -p /logs

# copy overlay
COPY --from=builder --chown=${USER}:${GROUP} cas-overlay/build/libs/cas.war ${CATALINA_BASE}/webapps/cas/cas.war

RUN set -x \
 && cd ${CATALINA_BASE}/webapps/cas/ \
 && unzip cas.war \
 && rm -f cas.war \
 && chown -R ${USER}:${GROUP} ${CATALINA_BASE}

# copy resources
COPY --chown=${USER}:${GROUP} resources /

RUN chown -R ${USER}:${GROUP} /etc/cas /logs ${SSL_BASE_DIRECTORY}

# expose tomcat port
EXPOSE 8080

HEALTHCHECK CMD doguctl healthy cas || exit 1

USER cas

CMD /startup.sh

