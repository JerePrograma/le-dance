FROM node:22.14.0-alpine3.21 AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL
ARG VITE_APP_TIME_ZONE=America/Argentina/Buenos_Aires
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_APP_TIME_ZONE=$VITE_APP_TIME_ZONE
RUN npm run build

FROM nginx:1.27.4-alpine3.21

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
