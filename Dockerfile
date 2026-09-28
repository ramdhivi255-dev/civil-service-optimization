# Dockerfile — Apache Tomcat 9 Deployment Configuration

FROM tomcat:9.0-jdk11-openjdk-slim AS builder

WORKDIR /app

COPY . .

RUN mkdir -p WEB-INF/classes

RUN javac -encoding UTF-8 -cp "/usr/local/tomcat/lib/servlet-api.jar:WEB-INF/lib/*" -d WEB-INF/classes src/com/example/*.java

FROM tomcat:9.0-jdk11-openjdk-slim

WORKDIR /usr/local/tomcat

RUN rm -rf webapps/*

COPY --from=builder /app /usr/local/tomcat/webapps/ROOT

RUN echo '#!/bin/sh' > /usr/local/tomcat/bin/start-tomcat.sh && \
    echo 'PORT=${PORT:-8080}' >> /usr/local/tomcat/bin/start-tomcat.sh && \
    echo 'sed -i "s/port=\"8080\"/port=\"$PORT\"/g" /usr/local/tomcat/conf/server.xml' >> /usr/local/tomcat/bin/start-tomcat.sh && \
    echo 'exec catalina.sh run' >> /usr/local/tomcat/bin/start-tomcat.sh && \
    chmod +x /usr/local/tomcat/bin/start-tomcat.sh

EXPOSE 8080

CMD ["/usr/local/tomcat/bin/start-tomcat.sh"]
