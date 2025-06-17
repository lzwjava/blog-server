# blog-server

[![Java CI with Maven](https://github.com/lzwjava/blog-server/actions/workflows/maven.yml/badge.svg)](https://github.com/lzwjava/blog-server/actions/workflows/maven.yml)

A blog server built with Node.js and Express, designed to manage blog posts, comments, and user accounts.

## Installation

To install the necessary dependencies, run the following command:

```bash
sudo apt install vnstat
sudo apt install pandoc 
sudo apt install -y fonts-noto
sudo apt install -y fonts-noto-cjk
sudo apt install -y fonts-dejavu
          
mvn -X compile

mvn checkstyle:check

mvn spotless:apply

mvn spring-boot:run
```

## License

Distributed under the MIT License. See `LICENSE` for more details.

