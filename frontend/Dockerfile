# Usa una imagen con Node.js para construir
FROM node:22 AS build-stage

# Directorio de trabajo
WORKDIR /app

# Copia los archivos y aprovecha el caché de dependencias
COPY package.json package-lock.json ./
RUN npm install

# Copia el resto del código
COPY . .

# Compila la app
RUN npm run build

# Usa una imagen Nginx ligera para servir el frontend
FROM nginx:alpine AS production-stage

# Copia la build del frontend a Nginx
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Copia la configuración personalizada de Nginx
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Exponer el puerto 80
EXPOSE 80

# Arranca Nginx
CMD ["nginx", "-g", "daemon off;"]

