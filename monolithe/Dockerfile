# --- STAGE 1: Build ---
# Utilise une image Maven avec Java 17 pour construire le projet.
FROM maven:3.9-eclipse-temurin-17 AS build

# Définit le répertoire de travail à l'intérieur du conteneur.
WORKDIR /app

# Copie d'abord le pom.xml pour tirer parti du cache de Docker.
# Les dépendances ne seront téléchargées que si le pom.xml change.
COPY pom.xml .
RUN mvn dependency:go-offline

# Copie le reste du code source de l'application.
COPY src ./src

# --- MODIFICATION CI-DESSOUS ---
# Construit l'application SANS exécuter les tests.
# La validation des tests est le rôle de la CI ou du développeur en local.
RUN mvn clean package -DskipTests


# --- STAGE 2: Run ---
# Utilise une image Java 17 JRE minimale pour l'exécution.
# C'est beaucoup plus léger et plus sécurisé qu'un JDK complet.
FROM eclipse-temurin:17-jre-jammy

# Définit le répertoire de travail.
WORKDIR /app

# Copie uniquement le .jar construit à l'étape précédente depuis le stage "build".
COPY --from=build /app/target/brokerx-1.0.0.jar .

# Expose le port 8080, sur lequel Spring Boot s'exécute par défaut.
EXPOSE 8080

# Commande à exécuter lorsque le conteneur démarre.
ENTRYPOINT ["java", "-jar", "brokerx-1.0.0.jar"]