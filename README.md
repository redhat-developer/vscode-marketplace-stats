# VS Code Marketplace statistics

This Quarkus application scraps the [Visual Studio Code marketplace](https://marketplace.visualstudio.com/) to track the evolution of VS Code extensions downloads.

## Running the application

Use Docker Compose to boot a PostgreSQL DB and the latest [Docker Image](https://hub.docker.com/r/fbricon/vscode-marketplace-stats/tags) of this application.

```shell
docker-compose up
```

## Running the application in dev mode

The application will start a PostgreSQL DB using [DevServices](https://quarkus.io/guides/datasource#dev-services-configuration-free-databases).

Run the following command to start the application (make sure your Docker daemon is running):

```shell script
./mvnw clean compile quarkus:dev -Ddebug
```

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
 ./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/vscode-marketplace-0.1.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

### Manually deploy to OpenShift
After deploying an postgresql database, add the template to the OpenShift cluster:
> oc apply -f openshift/template.yaml
```
➜  oc apply -f openshift/template.yaml
template.template.openshift.io/vscode-marketplace-stats configured
```

Create a new application from the template:
> oc new-app vscode-marketplace-stats -p SERVICE_ACCOUNT=<service-account>
```
➜  oc new-app vscode-marketplace-stats -p SERVICE_ACCOUNT=<service-account>
--> Deploying template "fbricon-dev/vscode-marketplace-stats" to project fbricon-dev

     * With parameters:
        * IMAGE=fbricon/vscode-marketplace-stats
        * IMAGE_TAG=latest
        * CPU_REQUEST=400m
        * CPU_LIMIT=1000m
        * MEMORY_REQUEST=768Mi
        * Memory limit=1Gi
        * REPLICAS=1
        * SERVICE_ACCOUNT=<service-account>

--> Creating resources ...
deploymentconfig.apps.openshift.io "vscode-marketplace-stats" created
service "vscode-marketplace-stats" created
--> Success
Application is not exposed. You can expose services to the outside world by executing one or more of the commands below:
'oc expose service/vscode-marketplace-stats'
Run 'oc status' to view your app.
```

Expose an HTTPS route to the application:
> oc create route edge --service service/vscode-marketplace-stats
```
➜  oc create route edge --service service/vscode-marketplace-stats
route.route.openshift.io/vscode-marketplace-stats created
```