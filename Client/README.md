# Client
## JaCoCo

`mvn jacoco:report`
Open *target/site/jacoco/index.html*

Resthome4LogsAppender: 97% instructions and 87% branch coverage.
- append: The (closed || log==null) condition misses 1/4 branches but the branch where they are both true is unnecessary because it is an OR condition.
- postLogsbatch: There is a catch statement above that will trigger if the log is imparsable to JSON. This is likely never going to be triggered because of the above
condition that stops null logs from reaching it. A reason why it has to stay is if one of the libraries I am using throws an exception for no fault of mine.

## JDepend
`mvn site`
Open *target/site/jdepend-report.html*

Distance to main path: 0

The package is coincident with the main sequence. There is no abstraction in the package.

## Spotbugs
`mvn spotbugs:spotbugs`
`mvn spotbugs:gui`

0 bugs found for the Client.

## Run Random Generator
From your command line run:

`mvn clean package`

`java -jar target/random.jar`

## Run LogMonitor
From your command line run:

`mvn clean package`

`java -jar target/logMonitor.jar`
