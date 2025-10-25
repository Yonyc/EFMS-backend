set -a
. .env
set +a

mvn clean spring-boot:run -Dspring-boot.run.profiles=${SPRING_PROFILES_ACTIVE}