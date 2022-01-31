FROM gradle:7.3.3-jdk17 as builder

WORKDIR /build

COPY build.gradle /build
COPY gradle.properties /build
COPY src /build/src

RUN gradle shadowJar
RUN ls -al /build/build/libs

FROM openjdk:17
COPY --from=builder "/build/build/libs/build-1.0.0-all.jar" "CurseTracker-1.0.0-all.jar"

CMD ["java", "-jar", "CurseTracker-1.0.0-all.jar"]