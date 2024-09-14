package src.features.lab1;

public class Main {

    public static void main(String args[]) {

        final IWordCounter counter = new WordCounterImpl();

        final ITextLoader textLoader = new TextFileLoader("src/features/lab1/example_text.txt");

        try {
            final String exampleText = textLoader.load();
            var map = counter.calculate(exampleText);
            System.out.println(map);

        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
