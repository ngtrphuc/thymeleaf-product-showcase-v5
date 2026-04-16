FROM node:24-alpine
WORKDIR /workspace

COPY package*.json ./
COPY apps/web/package*.json ./apps/web/
COPY packages/shared/package*.json ./packages/shared/

RUN npm install

COPY apps/web ./apps/web
COPY packages/shared ./packages/shared

WORKDIR /workspace/apps/web
EXPOSE 3000
CMD ["npm", "run", "dev"]
