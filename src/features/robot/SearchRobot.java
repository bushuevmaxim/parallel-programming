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

public class SearchRobot {

    public static void main(String[] args) {
        String directoryPath = "/Users/max/Documents/spring-framework-main";
        Map<String, List<String>> inheritanceIndex = new HashMap<>();

        try {
            List<String> javaFiles = Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .map(Path::toString)
                    .collect(Collectors.toList());

            CountDownLatch latch = new CountDownLatch(javaFiles.size());

            for (String filePath : javaFiles) {
                new Thread(() -> {
                    processFile(filePath, inheritanceIndex);
                    latch.countDown();
                }).start();
            }

            latch.await();
            printInheritanceIndex(inheritanceIndex);
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
    }

    private static void processFile(String filePath, Map<String, List<String>> inheritanceIndex) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            String className = null;

            for (String line : lines) {
                line = line.trim();
                Matcher classMatcher = Pattern.compile("^(class|interface)\\s+(\\w+)").matcher(line);
                if (classMatcher.find()) {
                    className = classMatcher.group(2);
                }
                Matcher extendsMatcher = Pattern.compile("extends\\s+(\\w+)").matcher(line);
                if (extendsMatcher.find() && className != null) {
                    String parentClass = extendsMatcher.group(1);

                    final List<String> classes = inheritanceIndex.getOrDefault(parentClass, new ArrayList<>());
                    classes.add(className);
                    inheritanceIndex.put(parentClass, classes);
                }

                Matcher implementsMatcher = Pattern.compile("implements\\s+([\\w\\s,]+)").matcher(line);
                if (implementsMatcher.find() && className != null) {
                    String[] interfaces = implementsMatcher.group(1).split(",");
                    for (String iface : interfaces) {
                        final List<String> classes = inheritanceIndex.getOrDefault(iface.trim(), new ArrayList<>());
                        classes.add(className);
                        inheritanceIndex.put(iface.trim(), classes);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static void printInheritanceIndex(Map<String, List<String>> inheritanceIndex) {
        inheritanceIndex.forEach((key, value) -> {
            System.out.println(key + " -> " + value.stream().collect(Collectors.joining(", ")));
        });
    }
}
