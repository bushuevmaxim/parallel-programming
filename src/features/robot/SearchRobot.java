package src.features.robot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchRobot {

    public static void main(String[] args) {
        String projectPath = "unknownpackage";
        buildInheritanceIndex(projectPath);
    }

    public static void buildInheritanceIndex(String projectPath) {

        try {
            Map<String, List<String>> inheritanceIndex = new HashMap<>();
            try (Stream<Path> pathStream
                    = Files.walk(Paths.get(projectPath))) {

                List<Path> filePathes = pathStream.filter(Files::isRegularFile).toList();

                int countOfFiles = filePathes.size();

                CountDownLatch latch = new CountDownLatch(countOfFiles);

                System.out.println(countOfFiles);

                for (Path filePath : filePathes) {

                    Thread thread = new Thread(() -> {

                        try {
                            final String fileContent = processFile(filePath);

                            final String childEntityName = parseChildName(fileContent);

                            final List<String> parents = parseParentsNames(fileContent);

                            final List<String> classes = inheritanceIndex.getOrDefault(fileContent, new ArrayList<>());

                            classes.addAll(parents);

                            inheritanceIndex.put(childEntityName, classes);
                        } finally {
                            latch.countDown();
                        }
                    });

                    thread.start();

                }

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    System.out.println(e);
                }

            }

            inheritanceIndex.forEach((className, parents) -> {
                System.out.println(className + " -> "
                        + parents);
            });
        } catch (IOException e) {
            System.out.println(e);

        }
    }

    public static String processFile(Path path) {

        final String file = load(path);

        final Boolean isValidFile = filterFile(file);

        if (!isValidFile) {
            return "";
        }

        return file;

    }

    private static Boolean filterFile(String fileContent) {

        return fileContent.contains("class")
                || fileContent.contains("interface")
                || fileContent.contains("extends")
                || fileContent.contains("implements");
    }

    private static String load(Path path) {

        var text = "";
        try {
            text = new String(
                    Files.readAllBytes(path));
        } catch (IOException e) {
            System.out.println(e);

        }

        return text;
    }

    private static String parseChildName(String fileContent) {

        Pattern pattern = Pattern.compile("class\\s+(\\w+)|interface\\s+(\\w+)");
        Matcher matcher = pattern.matcher(fileContent);
        var childName = "";
        if (matcher.find()) {
            childName = matcher.group();
        }

        return childName;
    }

    private static List<String> parseParentsNames(String fileContent) {

        Pattern pattern
                = Pattern.compile("extends\\s+(.+)|implements\\s+(.+)");
        Matcher matcher = pattern.matcher(fileContent);

        List<String> parentNames = new ArrayList<>();

        if (matcher.find()) {
            String match = matcher.group();

            List<String> rawParentNames = new ArrayList<>(Arrays.asList(match.split("extends\\s+|implements\\s+|\\{")));

            List<String> filteredParentNames = rawParentNames.stream().map(String::trim).filter((string) -> {
                return !string.isEmpty();
            }).collect(Collectors.toList());

            parentNames.addAll(filteredParentNames);

        }
        return parentNames;
    }
}
