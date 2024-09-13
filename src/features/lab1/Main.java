package src.features.lab1;

public class Main {

    public static void main(String args[]) {

        IWordCounter counter = new WordCounterImpl();

        String exampleText = "На сегодняшний момент язык Java является одним из самых распространенных и популярных языков программирования. Первая версия языка появилась еще в 1996 году в недрах компании Sun Microsystems, впоследствии поглощенной компанией Oracle. Java задумывался как универсальный язык программирования, который можно применять для различного рода задач. И к настоящему времени язык Java проделал большой путь, было издано множество различных версий. Текущей версией является Java 22, которая вышла в марте 2024 года.";

        var map = counter.calculate(exampleText);
        System.out.println(map);
    }
}
