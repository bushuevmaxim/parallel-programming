package src.features.lab1;

import java.util.HashMap;
import java.util.Map;

public class WordCounterImpl implements IWordCounter {

    private static final String WORD_SPLIT_REG_EXP = "\\s*(\\s|,|!|\\.)\\s*";

    @Override
    public Map<String, Integer> calculate(String text) {

        String[] words = text.split(WORD_SPLIT_REG_EXP);

        Map<String, Integer> countOfWords = new HashMap<>();
        for (String word : words) {

            final int previousValue = countOfWords.getOrDefault(word, 0);

            final int actualValue = previousValue + 1;

            countOfWords.put(word, actualValue);

        }

        return countOfWords;
    }

}
