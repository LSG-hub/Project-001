# Next.js website for Cloud Run. Build from the repo root:
#   docker build -f apps/web/Dockerfile -t web .
FROM node:22-alpine AS build
WORKDIR /repo
COPY package.json package-lock.json ./
COPY apps/web/package.json apps/web/
RUN npm ci --workspace @project001/web --include-workspace-root
COPY apps/web apps/web
RUN npm run build --workspace @project001/web

FROM node:22-alpine AS run
WORKDIR /app
ENV NODE_ENV=production PORT=8080 HOSTNAME=0.0.0.0 WAITLIST_STORE=firestore
RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /repo/apps/web/.next/standalone ./
COPY --from=build --chown=app:app /repo/apps/web/.next/static ./apps/web/.next/static
COPY --from=build --chown=app:app /repo/apps/web/public ./apps/web/public
USER app
EXPOSE 8080
CMD ["node", "apps/web/server.js"]
