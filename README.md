# blog-server

[![Java CI with Maven](https://github.com/lzwjava/blog-server/actions/workflows/maven.yml/badge.svg)](https://github.com/lzwjava/blog-server/actions/workflows/maven.yml)

A note creation server that accepts note content via REST API, writes it to system clipboard, and triggers Python-based note creation.

## Installation

### Prerequisites

For clipboard operations on Linux, install `xclip` or `xsel`:
```bash
sudo apt install xclip xsel  # or whichever is available
```

### Build and Run

```bash
# Build the project
mvn clean compile

# Run checkstyle and code formatting
mvn checkstyle:check
mvn spotless:apply

# Start the server
mvn spring-boot:run
```

## Configuration

The server requires the following environment variables:

### BLOG_SOURCE_PATH
Path to the blog-source repository containing the `create_note_from_clipboard.py` script.

```bash
export BLOG_SOURCE_PATH=/path/to/your/blog-source-repo
mvn spring-boot:run
```

### Spring Boot Configuration

You can also set this in `application.properties`:
```properties
blog.source.path=/path/to/your/blog-source-repo
```

And then load it as a property in the controller:
```java
String scriptPath = System.getProperty("blog.source.path", "/path/to/blog-source");
```

## API Usage

### Create Note
POST `/create-note`

Request body:
```json
{
  "content": "Your note content here",
  "model": "mistral-medium"  // optional, defaults to mistral-medium
}
```

## Flutter iOS App

A companion Flutter app is available at `/Users/lzwjava/projects/lzwjava_blog` for iOS note uploading.

Make sure to update the API URL in `lzwjava_blog/lib/config.dart` to point to this server.

## License

Distributed under the MIT License. See `LICENSE` for more details.

