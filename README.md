# Proxy App

This project is a Quarkus-based proxy application. It fetches remote HTML content, rewrites links so that all navigation remains within the proxy, and appends a trademark symbol (™) to every six-letter word in the text content.

## Features

- **Dynamic Target Domain:**  
  The proxy defaults to `https://quarkus.io` but can be configured via the `target` query parameter (e.g., `?target=https://pt.quarkus.io`).

- **HTML Modification:**  
  The application processes HTML responses to:
  - Append "™" to every six-letter word.
  - Rewrite both absolute and relative links so that they point back through the proxy by appending a query parameter that preserves the original target domain.

- **Redirect Handling:**  
  When the target server returns a redirect (HTTP 3xx), the proxy rewrites the `Location` header so that subsequent navigation remains within the proxy.

## Prerequisites

- Java 17 or later
- Gradle
- Git

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/yourusername/proxy-app.git
cd proxy-app
```

### Build the Application
## Use the Gradle wrapper to build the project:
```
./gradlew clean build
```

### Run the Application in Development Mode
## Quarkus supports live coding and hot reloading. Start the application in dev mode:
```
./gradlew quarkusDev
```

### Running the Application
## Once the application is running (by default on port 8080), test it using your web browser:
1. Default Proxy:
Open a browser and navigate to:
http://localhost:8080/
This will proxy requests to https://quarkus.io.

2. Dynamic Target Domain:
To proxy to a different target (e.g., the Portuguese site), navigate to:
http://localhost:8080/?target=https://pt.quarkus.io

3. Link Rewriting:
The application rewrites links in the returned HTML so that when you click a link, it routes through the proxy with the appropriate target query parameter.


### Testing
## The project includes unit and integration tests. To run tests using Gradle, use:

```
./gradlew test
```

Tests use RestAssured (and optionally MockWebServer) to simulate external responses and validate that both the HTML modification and proxy functionality work as expected.

