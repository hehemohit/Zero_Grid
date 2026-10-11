FROM node:20-alpine

# Install build dependencies for native Node addons (bcrypt)
RUN apk add --no-cache libc6-compat python3 make g++

WORKDIR /app

# Install production dependencies
COPY package*.json ./
RUN npm ci --omit=dev

# Copy backend source
COPY src/ ./src/

# Configuration
ENV PORT=5000
ENV NODE_ENV=production

EXPOSE 5000

# Healthcheck probe matching backend /health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5000/health || exit 1

CMD ["node", "src/server.js"]
