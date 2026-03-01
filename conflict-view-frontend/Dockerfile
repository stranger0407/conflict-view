FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build:prod

FROM node:20-alpine
WORKDIR /app
RUN npm install -g serve
COPY --from=build /app/dist/conflict-view-frontend/browser ./dist
ENV PORT=3000
EXPOSE $PORT
CMD sh -c "serve -s dist -l \$PORT"
