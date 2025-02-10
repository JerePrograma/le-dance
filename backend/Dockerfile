# Imagen base con JDK 17 (Corretto)
FROM amazoncorretto:17

# Directorio de trabajo
WORKDIR /app

# Copiar el JAR del backend
COPY target/backend-1.0.jar app.jar

# Exponer el puerto donde correrá la app
EXPOSE 8080

# Comando de inicio del backend
ENTRYPOINT ["java", "-jar", "app.jar"]
