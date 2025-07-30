@echo off
title EnsaChat Project Runner
echo Starting EnsaChat Server and 2 Clients...
echo.

:: Start server
start "Server" cmd /k java -cp "EnsaChatServer.jar;lib/sqlite-jdbc-3.49.1.0.jar" server.ServerMain

:: Wait a little before launching clients (give server time to start)
timeout /t 2 /nobreak >nul

:: Start first client
start "Client 1" cmd /k java --module-path lib/javafx-sdk-24.0.2/lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp "EnsaChatClient.jar;lib/sqlite-jdbc-3.49.1.0.jar" clients.AppLauncher

:: Start second client
start "Client 2" cmd /k java --module-path lib/javafx-sdk-24.0.2/lib --add-modules javafx.controls,javafx.fxml,javafx.media -cp "EnsaChatClient.jar;lib/sqlite-jdbc-3.49.1.0.jar" clients.AppLauncher

echo All components launched.
