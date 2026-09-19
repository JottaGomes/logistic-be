# Build stage
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Serve stage
FROM nginx:alpine
COPY --from=build /app/dist/dachser-logistics-ui/browser /usr/share/nginx/html/dachser
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
