## Getting Started

Welcome to ENSA CHAT this project is made by Yassine El Atlassi (yassinelatlassi@gmail.com)

## TO RUN

A. You can just click on the run_all.bat (It runs 1 instance of the server, and 2 of the client )

OR

B. Go to the root project and run:

Server : java -cp "EnsaChatServer.jar;lib/sqlite-jdbc-3.49.1.0.jar" server.ServerMain

Client : java --module-path lib/javafx-sdk-24.0.2/lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp "EnsaChatClient.jar;lib/sqlite-jdbc-3.49.1.0.jar" clients.AppLauncher
