package com.superapp.downloader;

import static java.net.http.HttpClient.Redirect.ALWAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

public class App {

    private static Path dest;
    private static String token;
    private static DateTimeFormatter cmdParameterFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static DateTimeFormatter papertrailDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static HttpClient client = HttpClient.newBuilder()
            .followRedirects(ALWAYS)
            .build();

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options()
                .addOption("token", true, "Papertrail API token. (Papertrail > Settings > Profile: API Token)")
                .addOption("from", true, "Start datetime (eg: 2019-01-03 10:00)")
                .addOption("to", true, "End datetime (optional) (eg: 2019-01-03 11:00)")
                .addOption("dest", true, "Destination folder");

        CommandLine line = parser.parse(options, args);

        if (!line.hasOption("token") || !line.hasOption("from") || !line.hasOption("dest")) {
            String cmdLineSyntax = "java -jar papertrail-archive-downloader-1.0.0.jar -from \"2019-01-01 00:00\" -to \"2019-02-01 00:00\" -dest /tmp -token dgcKE8AJKNDFSD345CheNO\n\n";
            new HelpFormatter().printHelp(500, cmdLineSyntax, "", options, "");
            return;
        }

        token = line.getOptionValue("token");
        dest = Paths.get(line.getOptionValue("dest"));

        var ini = LocalDateTime.parse(line.getOptionValue("from"), cmdParameterFormatter);

        String toValue = line.getOptionValue("to");
        var end = toValue != null ? LocalDateTime.parse(toValue, cmdParameterFormatter) : LocalDateTime.now();

        if (ini.isAfter(end)) {
            System.err.println("Start datetime invalid");
            return;
        }

        var totalFiles = HOURS.between(ini, end);
        var executor = Executors.newFixedThreadPool(5);

        while (ini.isBefore(end)) {
            var fileDate = ini;
            executor.submit(() -> download(fileDate));
            ini = ini.plus(Duration.ofHours(1));
        }

        executor.shutdown();
        System.out.printf("Completed %s\n", executor.awaitTermination(totalFiles, MINUTES));
    }

    private static void download(LocalDateTime date) {
        var dateFmt = date.format(papertrailDateFormatter);

        try {
            Files.createDirectories(dest);
        } catch (IOException e) {
            System.err.printf("Error while accessing %s\n", dest);
            System.exit(-1);
        }

        var path = dest.resolve(dateFmt + ".tsv.gz");
        var request = HttpRequest.newBuilder(URI.create("https://papertrailapp.com/api/v1/archives/" + dateFmt + "/download"))
                .headers("X-Papertrail-Token", token)
                .build();

        HttpResponse<Path> response = null;
        for (var i = 1; response == null || response.statusCode() == 429; i++) {
            try {
                response = client.send(request, BodyHandlers.ofFile(path));
                if (response.statusCode() == 429) {
                    System.out.printf("%s Too many requests, trying again. Http Status:%s\n", path, response.statusCode());
                    Thread.sleep(i * 700);
                    continue;
                }
                System.out.printf("%s downloaded. Http Status:%s\n", path, response.statusCode());
            } catch (Exception e) {
                System.err.printf("Error while downloading %s %s\n", path, e);
            }
        }
    }
}
