FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src/ src/
COPY data/ data/
RUN mvn package -q -DskipTests

FROM nginx:alpine
COPY webapp/ /usr/share/nginx/html/
COPY --from=build /app/target/algo-1.0.0.jar /app/algo.jar
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
