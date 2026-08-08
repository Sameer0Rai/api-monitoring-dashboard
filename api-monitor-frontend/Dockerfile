# ---- Build stage -------------------------------------------------------
FROM node:20-alpine AS build

WORKDIR /build

# Same dependency-layer-caching idea as the backend Dockerfile: copy only the
# manifest first so `npm ci` is only re-run when dependencies actually change.
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund

COPY . .

# Baked in at build time since Vite inlines VITE_* vars into the JS bundle.
# docker-compose passes this as a build arg so it can differ per environment.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build


# ---- Runtime stage ------------------------------------------------------
FROM nginx:1.27-alpine

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:80 >/dev/null 2>&1 || exit 1

CMD ["nginx", "-g", "daemon off;"]
