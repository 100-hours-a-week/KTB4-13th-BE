# 빌드 단계: Java 25 JDK와 Gradle로 실행 JAR 생성
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Gradle 설정과 Wrapper 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# 프로젝트 소스 복사 후 JAR 빌드
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon


# 실행 단계: 빌드에 사용하지 않는 Java 25 JRE만 사용
FROM eclipse-temurin:25-jre

WORKDIR /app

# 애플리케이션을 일반 사용자 권한으로 실행
RUN useradd --system --create-home --uid 1001 appuser

# 빌드 단계에서 생성된 실행 JAR만 복사
COPY --from=builder --chown=appuser:appuser \
     /app/build/libs/book.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
