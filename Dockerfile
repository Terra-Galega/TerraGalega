# ---------- Build ----------
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Railway pasa las variables del servicio como build args: API_URL = URL pública del back
ARG API_URL
RUN if [ -n "$API_URL" ]; then sed -i "s|__API_URL__|$API_URL|" src/app/api-url.ts; fi

RUN npm run build

# ---------- Servidor estático ----------
FROM nginx:alpine
# Railway define PORT; nginx lo toma de la plantilla (envsubst)
ENV PORT=80
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/angular/browser /usr/share/nginx/html
