package src.features.robot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchRobot {

    private static final ReentrantLock lock = new ReentrantLock();

    private static BlockingQueue<Callable<ReverseInheritanceIndex>> taskQueue;
    private static BlockingQueue<ReverseInheritanceIndex> resultsQueue;

    public static void main(String[] args) throws InterruptedException {
        String directoryPath = "/Users/max/Documents/spring-framework-main";
        ReverseInheritanceIndex inheritanceIndex = new ReverseInheritanceIndex();

        try {
            List<Path> javaFiles = Files.walk(Paths.get(directoryPath))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            int countTasks = javaFiles.size();
            taskQueue = new LinkedBlockingQueue<>(countTasks);

            for (Path filePath : javaFiles) {
                taskQueue.add(() -> processFile(filePath));
            }

            ExecutorService executor = Executors.newFixedThreadPool(Thread.activeCount());

            resultsQueue = new LinkedBlockingQueue<>(countTasks);

            CountDownLatch latch = new CountDownLatch(countTasks);

            for (Callable<ReverseInheritanceIndex> task : taskQueue) {

                ReverseInheritanceIndex partInheritedIndex;
                try {
                    partInheritedIndex = executor.submit(task).get();
                    resultsQueue.add(partInheritedIndex);
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println(e);
                } finally {
                    latch.countDown();
                }

            }

            latch.await();
            executor.shutdown();

            for (ReverseInheritanceIndex partInheritedIndex : resultsQueue) {

                updateInheritanceIndex(inheritanceIndex, partInheritedIndex);

            }

            printInheritanceIndex(inheritanceIndex);

        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static ReverseInheritanceIndex processFile(Path filePath) {
        ReverseInheritanceIndex inheritanceIndex = new ReverseInheritanceIndex();

        try {
            final String fileContent = new String(Files.readAllBytes(filePath)).replaceAll("//.+", "")
                    .replaceAll("//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "")
                    .replaceAll("\t+", " ")
                    .replaceAll("\\r?\\n", " ")
                    .replaceAll("(/\\*.*\\*/)", " ")
                    .replaceAll("\".+\"", "")
                    .replaceAll(" +", " ");;
            String className = null;

            List<String> matcherResult = Pattern.compile("( class | interface )([a-zA-Z ,0-9]+)(\\{)").matcher(fileContent).results()
                    .map(MatchResult::group)
                    .collect(Collectors.toList());

            for (String line : matcherResult) {
                line = line.trim();
                Matcher classMatcher = Pattern.compile("(class|interface)\\s+(\\w+)").matcher(line);
                if (classMatcher.find()) {
                    className = classMatcher.group(2);

                }
                Matcher extendsMatcher = Pattern.compile("extends\\s+(\\w+)").matcher(line);
                if (extendsMatcher.find() && className != null) {
                    String parentClass = extendsMatcher.group(1);

                    inheritanceIndex.update(parentClass, className);
                }

                Matcher implementsMatcher = Pattern.compile("implements\\s+([\\w\\s,]+)").matcher(line);
                if (implementsMatcher.find() && className != null) {
                    String[] interfaces = implementsMatcher.group(1).split(",");
                    for (String iface : interfaces) {

                        inheritanceIndex.update(iface.trim(), className);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        }

        return inheritanceIndex;
    }

    private static void printInheritanceIndex(ReverseInheritanceIndex inheritanceIndex) {
        inheritanceIndex.get().forEach((key, value) -> {
            System.out.println(key + " -> " + value.stream().collect(Collectors.joining(", ")));
        });
    }

    private static void updateInheritanceIndex(ReverseInheritanceIndex inheritanceIndex, ReverseInheritanceIndex partInheritanceIndex) {

        lock.lock();
        try {

            partInheritanceIndex.get().forEach((parent, children) -> {

                children.forEach(child -> inheritanceIndex.update(parent, child));
            });

        } finally {
            lock.unlock();
        }

    }

}

class ReverseInheritanceIndex {

    private final Map<String, List<String>> _map;

    public ReverseInheritanceIndex() {
        _map = new HashMap<>();
    }

    public Map<String, List<String>> get() {
        return _map;
    }

    public void update(String parent, String child) {

        final List<String> classes;
        classes = _map.getOrDefault(parent, new ArrayList<>());
        classes.add(child);
        _map.put(parent, classes);
    }

}
