FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
COPY index.html ./
COPY css ./css
COPY js ./js
EXPOSE 80
