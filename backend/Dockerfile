FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# dependency layer cached independently of source changes
COPY pom.xml ./
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -B -q -pl backend -am dependency:go-offline
COPY backend/src ./backend/src
RUN mvn -B -q -pl backend -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
