module.exports = {
  apps: [
    {
      name: "le-dance-backend",
      script: "java",
      args: `-jar ${process.env.LEDANCE_HOME}/backend/target/backend-1.0.jar`,
      env: {
        SPRING_PROFILES_ACTIVE: "prod",
        SPRING_DATASOURCE_URL: process.env.SPRING_DATASOURCE_URL,
        SPRING_DATASOURCE_USERNAME: process.env.SPRING_DATASOURCE_USERNAME,
        SPRING_DATASOURCE_PASSWORD: process.env.SPRING_DATASOURCE_PASSWORD,
        JWT_SECRET: process.env.JWT_SECRET,
      }
    }
  ]
};

