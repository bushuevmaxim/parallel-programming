package src.features.lab1;

import java.io.IOException;
import java.nio.file.*;

public class TextFileLoader implements ITextLoader {

    final String filePath;

    public TextFileLoader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String load() throws IOException {

        final String data = new String(
                Files.readAllBytes(Paths.get(this.filePath)));
        return data;

    }

}
