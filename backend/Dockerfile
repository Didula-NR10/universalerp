FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 erp
WORKDIR /app
COPY --from=build /build/target/backend-*.jar /app/erp.jar
USER erp
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/erp.jar"]
