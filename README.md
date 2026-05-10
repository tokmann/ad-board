# AdBoard App

AdBoard is a Java web application designed for advertisement management. It is created as a final project for the 
["Sental Courses"](https://xn--80atdl2c.xn--80ajtgsi.xn--p1ai/) course. This repository includes everything needed to 
build and run the application automatically using Docker.

### Prerequisites

Before you begin, ensure you have the following installed on your machine:
* Docker
* Docker Compose

### Installation & Deployment Steps

Follow these instructions to deploy the application:

#### Step 1: Clone the repository
Clone this repository to your local machine and navigate into the project root directory:
```bash
git clone https://github.com/tokmann/ad-board
cd adboard
```

#### Step 2: Build and start the services
Use Docker Compose to build the application and start the PostgreSQL database and Tomcat server. The --build flag ensures
that the application is freshly compiled from the source code.
```bash
docker-compose up -d --build
```

#### Step 3: Verify deployment
Wait a few moments for the database to initialize and Tomcat to deploy the .war file. You can check the logs to ensure 
everything started correctly:
```bash
docker-compose logs -f tomcat
```

#### Step 4: Access the application
Once the server is up and running, you can access the application via your web browser or API client (like Postman) at:

* Base URL: http://localhost:8080/adboard/api
* Swagger UI / API Docs: http://localhost:8080/adboard/swagger-ui/index.html

