# syntax=docker/dockerfile:1.3

FROM maven:3.8.1-openjdk-11 AS build 

WORKDIR /build 
# Set the timezone to Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && echo 'Asia/Ho_Chi_Minh' > /etc/timezone
# Set the locale to Vietnamese
RUN apt-get update 
RUN apt-get install -y locales 
RUN locale-gen vi_VN.UTF-8
RUN apt-get update 
#RUN update-locale LANG=vi_VN.UTF-8
# Set environment variables for locale
# Set the locale to Vietnamese

ENV LANG=vi_VN.UTF-8 \
    LANGUAGE=vi_VN:vi \
    LC_ALL=vi_VN.UTF-8

    
COPY . ./

RUN ls /build

RUN --mount=type=cache,target=/root/.m2 mvn clean install -f ./plugins/user_management/pom.xml


# 
FROM quay.io/keycloak/keycloak:18.0.2

LABEL maintainer="diennt"
LABEL description="Keycloak with User Management Plugin"
# Set the 
# ENV KC_DB=mysql
# ENV KC_DB_URL=jdbc:mysql://mysql-server:3306/keycloak
# ENV KC_DB_USERNAME=keycloak
# ENV KC_DB_PASSWORD=keycloakpass
# ENV KC_HOSTNAME=10.82.14.80

#ENV KC_HOSTNAME=10.82.14.80
#ENV KC_HOSTNAME_ADMIN=10.82.14.80:8080

#
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=change_me
#
ENV KC_PROXY=none
#ENV KC_HTTP_ENABLED=true
#ENV KC_PROXY_HEADERS=forwarded
ENV KC_PROXY_HEADERS=xforwarded
ENV PROXY_ADDRESS_FORWARDING=false
ENV KC_HTTP_ENABLED=false
#

ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
#

ENV KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/ssl/tls.crt
ENV KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/ssl/tls.key


COPY --from=build /build/ssl.crt /opt/keycloak/ssl/tls.crt
COPY --from=build /build/ssl.key /opt/keycloak/ssl/tls.key


# Expose HTTPS port
EXPOSE 8080
EXPOSE 8443
# Run Keycloak with HTTPS enabled
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", , "--hostname", "https://localhost:8443"]
#
CMD ["start-dev"]
#CMD ["start", "--hostname", "https://10.82.14.80:8443"]