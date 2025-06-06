# syntax=docker/dockerfile:1.3

FROM maven:3.8.1-openjdk-11 AS build 

WORKDIR /build 
# Set the timezone to Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/Asia/Ho_Chi_Minh /etc/localtime && echo 'Asia/Ho_Chi_Minh' > /etc/timezone
# Set the locale to Vietnamese
RUN apt-get update && apt-get install -y locales && \
    locale-gen vi_VN.UTF-8 && \
    update-locale LANG=vi_VN.UTF-8
ENV LANG=vi_VN.UTF-8 \
    LANGUAGE=vi_VN:vi \
    LC_ALL=vi_VN.UTF-8

    
COPY . ./

RUN ls /build

RUN --mount=type=cache,target=/root/.m2 mvn clean install -f ./plugins/user_management/pom.xml

FROM quay.io/keycloak/keycloak:18.0.2

LABEL maintainer="diennt"

ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=change_me
#
ENV KC_PROXY=none
#ENV KC_PROXY_HEADERS=forwarded
ENV KC_PROXY_HEADERS=xforwarded
ENV PROXY_ADDRESS_FORWARDING=false
ENV KC_HTTP_ENABLED=false
#
#ENV KC_HOSTNAME=https://10.82.14.80:8443
ENV KC_HOSTNAME_URL=https://10.82.14.80:8443
#ENV KC_HOSTNAME_ADMIN=https://10.82.14.80:8443
#ENV KC_HOSTNAME_ADMIN_URL=https://10.82.14.80:8443
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
#
#KC_HTTP_RELATIVE_PATH=/auth


# KC_HTTPS_CERTIFICATE_FILE: /opt/keycloak/conf/server.crt
# KC_HTTPS_CERTIFICATE_KEY_FILE: /opt/keycloak/conf/server.key

ENV KC_HTTPS_CERTIFICATE_FILE=/opt/keycloak/ssl/tls.crt
ENV KC_HTTPS_CERTIFICATE_KEY_FILE=/opt/keycloak/ssl/tls.key

# ENV KC_HTTP_ENABLED=false
# ENV KC_HTTPS_KEY_STORE_FILE=/etc/ssl/certs/ssl.p12
# ENV KC_HTTPS_KEY_STORE_PASSWORD=changeit

# Import SSL keys into a PKCS12 keystore
# RUN keytool -importkeystore \
#     -srckeystore /etc/ssl/private/ssl.key \
#     -srcstoretype PKCS12 \
#     -destkeystore /etc/ssl/certs/ssl.p12 \
#     -deststoretype PKCS12 \
#     -deststorepass changeit

#RUN sed -i -E "s/(<staticMaxAge>)2592000(<\/staticMaxAge>)/\1\-1\2/" /opt/jboss/keycloak/standalone/configuration/standalone.xml
#RUN sed -i -E "s/(<cacheThemes>)true(<\/cacheThemes>)/\1false\2/" /opt/jboss/keycloak/standalone/configuration/standalone.xml
#RUN sed -i -E "s/(<cacheTemplates>)true(<\/cacheTemplates>)/\1false\2/" /opt/jboss/keycloak/standalone/configuration/standalone.xml

#COPY --from=build /build/plugins/user_management/target/authentication-0.0.1-SNAPSHOT.jar  /opt/jboss/keycloak/standalone/deployments
#COPY --from=build /build/themes/ICS  /opt/jboss/keycloak/themes/ICS
#COPY --from=build /build/themes/base/login/login.ftl  /opt/jboss/keycloak/themes/base/login

#COPY --from=build /build/keycloak-config/My-Realm-realm.json  /opt/keycloak/data/import/
#COPY --from=build /build/keycloak-config/DCMS-Realm-export.json  /opt/keycloak/data/import/
#COPY --from=build /build/keycloak-config/DCMSDEV-Realm-export.json  /opt/keycloak/data/import/
#RUN /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/DCMS-Realm-export.json
#RUN /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/DCMSDEV-Realm-export.json

# Copy SSL certificate and private key to the container
#COPY /path/to/tls.crt /opt/keycloak/ssl/tls.crt
#COPY /path/to/tls.key /opt/keycloak/ssl/tls.key
COPY --from=build /build/ssl.crt /opt/keycloak/ssl/tls.crt
COPY --from=build /build/ssl.key /opt/keycloak/ssl/tls.key

#COPY --from=build /build//ssl.crt /opt/keycloak/ssl/tls.crt
#COPY --from=build /build/ssl.key /opt/keycloak/ssl/tls.key

# Expose HTTPS port
EXPOSE 8080
EXPOSE 8443
# Run Keycloak with HTTPS enabled
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start", , "--hostname", "https://localhost:8443"]
#
CMD ["start-dev"]
#CMD ["start", "--hostname", "https://10.82.14.80:8443"]