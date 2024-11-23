@ECHO off
if /I %1 == default goto :default
if /I %1 == install goto :install
if /I %1 == clean goto :clean
if /I %1 == run goto :run

goto :eof ::can be omitted to run the `default` function similarly to makefiles

:default
echo DEFAULT
goto :eof

:install
echo INSTALL
.\mvnw install
goto :eof

:clean
echo CLEAN
.\mvnw clean
goto :eof

:run
echo RUN
java -jar .\target\cognitive-analysis-0.0.1-SNAPSHOT.jar analyse '.\\src'
goto :eof