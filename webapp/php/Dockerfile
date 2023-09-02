FROM php:8.2-fpm-bullseye

# pm.max_childrenを変更する
RUN sed -i 's/pm.max_children = .*/pm.max_children = 40/' /usr/local/etc/php-fpm.d/www.conf
RUN sed -i 's/pm.start_servers = .*/pm.start_servers = 20/' /usr/local/etc/php-fpm.d/www.conf
RUN sed -i 's/pm.min_spare_servers = .*/pm.min_spare_servers = 20/' /usr/local/etc/php-fpm.d/www.conf
RUN sed -i 's/pm.max_spare_servers = .*/pm.max_spare_servers = 40/' /usr/local/etc/php-fpm.d/www.conf

RUN apt update && apt install -y \
  unzip libmemcached-dev zlib1g-dev

RUN docker-php-ext-install pdo pdo_mysql

RUN pecl install memcached \
  && docker-php-ext-enable memcached

RUN curl -sS https://getcomposer.org/installer | php \
  && mv composer.phar /usr/local/bin/composer

COPY ./composer.json /var/www/html
COPY ./composer.lock /var/www/html
WORKDIR /var/www/html

RUN composer install --no-dev

COPY . /var/www/html
