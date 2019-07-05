# Papertrail Archive Downloader

This application will help you downloading log archives from [Papertrail](https://papertrailapp.com/).
[Download compile jar here](https://www.dropbox.com/s/h1r344x1d0neuih/papertrail-archive-downloader-1.0.0.jar?dl=0)

### Requirements

  - +Java 12

### Usage: 
    java -jar papertrail-archive-downloader-1.0.0.jar -from "2019-01-01 00:00" -to "2019-02-01 00:00" -dest /tmp -token dgcKE8AJKN12345678

    -dest <arg>    Destination folder
    -from <arg>    Start datetime (eg: 2019-01-03 10:00)
    -to <arg>      End datetime (optional) (eg: 2019-01-03 11:00)
    -token <arg>   Papertrail API token. (Papertrail > Settings > Profile: API Token)

### Compiling

    mvn clean package

