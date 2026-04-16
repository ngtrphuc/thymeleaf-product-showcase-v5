FROM node:24-alpine
WORKDIR /workspace

COPY package*.json ./
COPY apps/api/package*.json ./apps/api/
COPY packages/shared/package*.json ./packages/shared/

RUN npm install

COPY apps/api ./apps/api
COPY packages/shared ./packages/shared

WORKDIR /workspace/apps/api
EXPOSE 4000
CMD ["npm", "run", "start:dev"]
