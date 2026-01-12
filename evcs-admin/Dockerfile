# Stage 1: Build frontend
# Default base images are public.
# For in-cluster Kaniko builds (air-gapped), override at build time, e.g.:
#   --build-arg NODE_IMAGE=192.168.20.235:5000/base/node:20-alpine
#   --build-arg NGINX_IMAGE=192.168.20.235:5000/base/nginx:alpine
ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:alpine

FROM ${NODE_IMAGE} AS build
ARG BUILD_NUMBER=0
ENV BUILD_NUMBER=${BUILD_NUMBER}

ARG GIT_COMMIT=unknown
ARG GIT_BRANCH=unknown
ENV GIT_COMMIT=${GIT_COMMIT}
ENV GIT_BRANCH=${GIT_BRANCH}
WORKDIR /app

# Copy package files for better caching
COPY package.json package-lock.json* ./

# Set npm mirror
RUN npm config set registry https://registry.npmmirror.com/

# Install dependencies
# IMPORTANT: `vue-tsc` is a devDependency, but the build step requires it.
# Some base images / environments may default to production-only installs.
RUN npm ci --include=dev || npm install --include=dev

# Copy source files
COPY . .
# Build the application (includes writing version.json)
RUN npm run build

# Stage 2: Runtime with Nginx
FROM ${NGINX_IMAGE}
WORKDIR /usr/share/nginx/html

# Remove default nginx static assets and config
RUN rm -rf /usr/share/nginx/html/* /etc/nginx/conf.d/default.conf

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf.template

# Render template at startup (no envsubst/gettext required)
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Copy built assets from build stage (including version.json)
COPY --from=build /app/dist .

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
        CMD wget --no-verbose --tries=1 --spider http://127.0.0.1/ || exit 1

# Start Nginx
ENTRYPOINT ["/docker-entrypoint.sh"]
