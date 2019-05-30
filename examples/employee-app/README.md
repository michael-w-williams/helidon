
# Helidon Quickstart SE - Employee Directory Example

This project implements an employee directory REST service using Helidon SE. The application is composed of a Helidon REST Microservice backend along with an HTML/JavaScript front end. The source for both application is included with the Maven project.

By default the service uses a ArrayList backend with sample data. You can connect the backend application to an Oracle database by changing the values in the `resources/DbCreds.properties` file.

## Prerequisites

1. Maven 3.5 or newer
2. Java SE 8 or newer
3. Docker 17 or newer to build and run docker images
4. Kubernetes minikube v0.24 or newer to deploy to Kubernetes (or access to a K8s 1.7.4 or newer cluster)
5. Kubectl 1.7.4 or newer to deploy to Kubernetes

Verify prerequisites

```sh
java -version
mvn --version
docker --version
minikube version
kubectl version --short
```

## Using Oracle DB

This application supports two storage implementations one using an array and other using an Oracle database to persist the data. To compile and package the application you need the JDBC driver. The JDBC driver for the Oracle DB is only available from the
https://www.oracle.com/webfolder/application/maven/index.html[Oracle Maven Repository].

This means that you have to configure the repository in order to add the driver
 to your classpath.

Follow these steps:

- Go to https://www.oracle.com/webfolder/application/maven/index.html and
 accept the license
- Create an OTN account
- Update your settings.xml as documented
 https://docs.oracle.com/middleware/1213/core/MAVEN/config_maven_repo.htm#MAVEN9016[here]
 

## Build

```sh
mvn package
```

## Start the application

```sh
cd target
java -jar employee-app.jar
```

**Note:** For the static elements of the application to work, you must start the application from the target directory.


## Exercise the application
Get all employees.
```json
curl -X GET curl -X GET http://localhost:8080/employees
```
Output:  
&lbrack;`{"birthDate":"1970-11-28T08:28:48.078Z","department":"Mobility","email":"Hugh.Jast@example.com","firstName":"Hugh","id":"100","lastName":"Jast","phone":"730-715-4446","title":"National Data Strategist"}`  
`. . .`

Get all employees whose last name contains "S".
```json
curl -X GET http://localhost:8080/employees/lastname/S
```
Output:  
&lbrack;`{"birthDate":"1978-03-18T17:00:12.938Z","department":"Security","email":"Zora.Sawayn@example.com","firstName":"Zora","id":"104","lastName":"Sawayn","phone":"923-814-0502","title":"Dynamic Marketing Designer"}`
`. . .`

Get an individual record.
```json
curl -X GET http://localhost:8080/employees/100
```
&lbrack;`{"birthDate":"1970-11-28T08:28:48.078Z","department":"Mobility","email":"Hugh.Jast@example.com","firstName":"Hugh","id":"100","lastName":"Jast","phone":"730-715-4446","title":"National Data Strategist"}`

Connect with a web brower at:
```json
http://localhost:8080/public/index.html
```


## Try health and metrics

```c
curl -s -X GET http://localhost:8080/health
{"outcome":"UP",...
. . .
```

### Prometheus Format

```c
curl -s -X GET http://localhost:8080/metrics
# TYPE base:gc_g1_young_generation_count gauge
. . .
```

### JSON Format
```c
curl -H 'Accept: application/json' -X GET http://localhost:8080/metrics
{"base":...
. . .
```

## Build the Docker Image

```sh
docker build -t employee-app .
```

## Start the application with Docker

```sh
docker run --rm -p 8080:8080 employee-app:latest
```

Exercise the application as described above.


## Deploy the application to Kubernetes

```
kubectl cluster-info                # Verify which cluster
kubectl get pods                    # Verify connectivity to cluster
kubectl create -f app.yaml   # Deply application
kubectl get service employee-app  # Get service info
```


###  Oracle DB Credentials
You can connect to two different datastores for the back end application. Just fill in the application.yaml files. To use an ArrayList as the data store, simply set `drivertype` to `Array`. To connect to an Oracle database, you must set all the values: `user`, `password`, `hosturl`, and `drivertype`. For Oracle, the `drivertype` should be set to `Oracle`.

```c
user=<user-db>
password=<password-user-db>
hosturl=<hostname>:<port>/<database_unique_name>.<host_domain_name>
drivertype=Array
```

## Create the database objects
1. Create a connection to your Oracle Database using sqlplus or SQL Developer. See https://docs.cloud.oracle.com/iaas/Content/Database/Tasks/connectingDB.htm.
2. Create the database objects:

      ```sql
      CREATE TABLE EMPLOYEE (
            ID INTEGER NOT NULL,
            FIRSTNAME VARCHAR(100),
            LASTNAME VARCHAR(100),
            EMAIL VARCHAR(100),
            PHONE VARCHAR(100),
            BIRTHDATE VARCHAR(10),
            TITLE VARCHAR(100),
            DEPARTMENT VARCHAR(100),
            PRIMARY KEY (ID)
            );
      ```

      ```sql
      CREATE SEQUENCE EMPLOYEE_SEQ
            START WITH     100
            INCREMENT BY   1;
      ```

      ```sql
      INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) VALUES (EMPLOYEE_SEQ.nextVal, 'Hugh', 'Jast', 'Hugh.Jast@example.com', '730-555-0100', '1970-11-28', 'National Data Strategist', 'Mobility');

      INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) VALUES (EMPLOYEE_SEQ.nextVal, 'Toy', 'Herzog', 'Toy.Herzog@example.com', '769-555-0102', '1961-08-08', 'Dynamic Operations Manager', 'Paradigm');

      INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) VALUES (EMPLOYEE_SEQ.nextVal, 'Reed', 'Hahn', 'Reed.Hahn@example.com', '429-555-0153', '1977-02-05', 'Future Directives Facilitator', 'Quality');

      INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) VALUES (EMPLOYEE_SEQ.nextVal, 'Novella', 'Bahringer', 'Novella.Bahringer@example.com', '293-596-3547', '1961-07-25', 'Principal Factors Architect', 'Division');

      INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) VALUES (EMPLOYEE_SEQ.nextVal, 'Zora', 'Sawayn', 'Zora.Sawayn@example.com', '923-555-0161', '1978-03-18', 'Dynamic Marketing Designer', 'Security');

      INSERT INTO EMPLOYEE (ID, FIRSTNAME, LASTNAME, EMAIL, PHONE, BIRTHDATE, TITLE, DEPARTMENT) VALUES (EMPLOYEE_SEQ.nextVal, 'Cordia', 'Willms', 'Cordia.Willms@example.com', '778-555-0187', '1989-03-31', 'Human Division Representative', 'Optimization');
      ```