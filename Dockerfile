ARG TOMCAT_MAJOR_VERSION=10
ARG TOMCAT_VERSION=10.1.26
ARG TOMCAT_TARGZ_SHA256=f73f76760137833b3305dfb18ed174f87feac3ab78f65289a0835a851d7cfeb2

FROM eclipse-temurin:21-jdk-alpine AS builder

RUN mkdir -p /cas-overlay
COPY ./app/gradle/ /cas-overlay/gradle/
COPY ./app/gradlew ./app/settings.gradle ./app/build.gradle ./app/gradle.properties ./app/lombok.config /cas-overlay/
WORKDIR /cas-overlay

# Cache gradle
RUN chmod 750 ./gradlew \
    && ./gradlew --version

# Cache dependencies
RUN ./gradlew --no-daemon dependencies

# Copy source code and build overlay
COPY ./app/src /cas-overlay/src/
RUN ./gradlew clean build --parallel --no-daemon

FROM registry.cloudogu.com/official/base:3.19.1-1 AS tomcat

ARG TOMCAT_MAJOR_VERSION
ARG TOMCAT_VERSION
ARG TOMCAT_TARGZ_SHA256

ENV TOMCAT_MAJOR_VERSION=${TOMCAT_MAJOR_VERSION} \
    TOMCAT_VERSION=${TOMCAT_VERSION} \
    TOMCAT_TARGZ_SHA256=${TOMCAT_TARGZ_SHA256}

RUN apk update && apk add wget && wget -O  "apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
  "http://archive.apache.org/dist/tomcat/tomcat-${TOMCAT_MAJOR_VERSION}/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
  && echo "${TOMCAT_TARGZ_SHA256} *apache-tomcat-${TOMCAT_VERSION}.tar.gz" | sha256sum -c - \
  && gunzip "apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
  && tar xf "apache-tomcat-${TOMCAT_VERSION}.tar" -C /opt \
  && rm "apache-tomcat-${TOMCAT_VERSION}.tar"




# registry.cloudogu.com/official/cas
FROM registry.cloudogu.com/official/java:21.0.3-1

LABEL NAME="official/cas" \
      VERSION="7.0.5.1-1" \
      maintainer="hello@cloudogu.com"

ARG TOMCAT_VERSION

# update packages of the image
RUN set -o errexit \
  && set -o nounset \
  && set -o pipefail \
  && apk update \
  && apk upgrade

# configure environment
ENV TOMCAT_VERSION=${TOMCAT_VERSION} \
	CATALINA_BASE=/opt/apache-tomcat \
	CATALINA_PID=/var/run/tomcat7.pid \
	CATALINA_SH=/opt/apache-tomcat/bin/catalina.sh \
	SERVICE_TAGS=webapp \
	USER=cas \
    GROUP=cas \
    SSL_BASE_DIRECTORY="/etc/ssl"

# setup user
RUN set -x \
 # create group and user for cas
 && addgroup -S -g 1000 ${GROUP} \
 && adduser -S -s /bin/bash -G ${GROUP} -u 1000 ${USER}

## copy tomcat \
COPY --from=tomcat /opt/apache-tomcat-${TOMCAT_VERSION} ${CATALINA_BASE}

## configure tomcat
RUN rm -rf ${CATALINA_BASE}/webapps/* \
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

