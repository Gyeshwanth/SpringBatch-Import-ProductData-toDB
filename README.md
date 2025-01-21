# Spring Boot Batch Processing CSV

## Overview
This project demonstrates a Spring Boot application that processes CSV files using Spring Batch. The application reads product data from a CSV file, processes it, and stores it in an H2 in-memory database.

## Features
- Upload CSV files containing product data.
- Process and validate CSV data using Spring Batch.
- Store processed data in an H2 in-memory database.
- Monitor batch job execution status.

## Prerequisites
- Java 17 or higher.
- Maven 3.6.0 or higher.

## Getting Started

### Clone the Repository
```sh
git clone https://github.com/your-username/your-repo-name.git
cd your-repo-name
```

### Build the Project
```sh
mvn clean install
```

### Run the Application
```sh
mvn spring-boot:run
```

### Access the Application
- **Application**: The application will be available at http://localhost:2001
- **H2 Console**: Access the H2 database console at http://localhost:2001/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - User: `root`
  - Password: `root`

## API Endpoints

### Upload CSV File
- **URL**: `/products/import`
- **Method**: `POST`
- **Request Param**: `file` (MultipartFile)
- **Description**: Upload a CSV file containing product data to be processed.

## Configuration

### Application Properties
Configuration settings are located in `src/main/resources/application.properties`.

### Example CSV Format
```csv
name,description,price,quantity
Product1,Description1,10.0,100
Product2,Description2,20.0,200
```

## Additional Notes
This `README.md` file provides an overview of the project, setup instructions, API endpoints, and other relevant information. Adjust the content as needed to fit your specific project details.

