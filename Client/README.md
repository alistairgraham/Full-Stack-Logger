## Random Generator
This executable sends a randomly generated log to the server every second.

From your command line run:

`mvn clean package`

`java -jar target/random.jar`

## Log Monitor
Log Monitor is a GUI that displays the logs from the server depending on the options you select. It also has a button that downloads the logs from the server to your local drive in the .xls format.

From your command line run:

`mvn clean package`

`java -jar target/logMonitor.jar`
