module.exports = {
  apps: [
    {
      name: "le-dance-backend",
      script: "java",
      args: "-jar /home/jeremias/le-dance/backend/target/backend-1.0.jar",
      env: {
        SPRING_DATASOURCE_URL: "jdbc:postgresql://localhost:5432/ledance_db",
        SPRING_DATASOURCE_USERNAME: "postgres",
        SPRING_DATASOURCE_PASSWORD: "root",
        JWT_SECRET: "TuValorSecretoMuyFuerte"
      }
    }
  ]
};

