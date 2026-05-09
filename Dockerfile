# 빌드 스테이지
FROM gradle:8.14-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 보안상 root 말고 별도 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
