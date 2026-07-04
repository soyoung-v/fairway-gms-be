# ─────────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 의존성 캐시 활용: 소스보다 먼저 Gradle 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew

# 의존성 먼저 다운로드 (소스 변경 없으면 캐시 재사용)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ─────────────────────────────────────────────
# Stage 2: Runtime (slim image)
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 JAR만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 앱 포트
EXPOSE 8080

# prod 프로필로 실행 (환경변수로 설정 주입)
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
