package com.textual_data_wrangling_101;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import java.io.*;
import java.util.Date;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static java.lang.Math.pow;
import static java.lang.String.format;

interface GenerateSampleDataConfig {

    @Option
    int getSamples();

    @Option
    File getFile();
    @SuppressWarnings("unused") boolean isFile();
}

public class GenerateSampleData {

    private static final Random random = new Random();

    private enum OUTCOME {
        SUCCESS, FAILURE
    }

    private enum DOCUMENT_TYPE {
        MEMORIA, CONTRATTO, PARERE, ATTO_DI_CITAZIONE
    }

    private enum ERROR_TYPE {
        FILE_NOT_FOUND, CANNOT_OPEN_FILE, UNSUPPORTED_ENCODING
    }

    private static boolean isSampleSuccessful() {
        return random.nextInt(100) < 80;
    }

    private static DOCUMENT_TYPE documentTypeOfSample() {
        int randomIntegerIn0_100 = random.nextInt(100);
        if (randomIntegerIn0_100 < 40) return DOCUMENT_TYPE.MEMORIA;
        if (randomIntegerIn0_100 < 70) return DOCUMENT_TYPE.CONTRATTO;
        if (randomIntegerIn0_100 < 85) return DOCUMENT_TYPE.PARERE;
        return DOCUMENT_TYPE.ATTO_DI_CITAZIONE;
    }

    private static ERROR_TYPE errorTypeOfSample() {
        int randomIntegerIn0_100 = random.nextInt(100);
        if (randomIntegerIn0_100 < 80) return ERROR_TYPE.FILE_NOT_FOUND;
        if (randomIntegerIn0_100 < 95) return ERROR_TYPE.CANNOT_OPEN_FILE;
        return ERROR_TYPE.UNSUPPORTED_ENCODING;
    }

    private static void printSample(int index, PrintWriter printWriter) {
        long millis = (long)(Math.sqrt(pow(1f + random.nextGaussian(), 2f)) * 300f);
        String now = new Date().toString();
        OUTCOME outcome = isSampleSuccessful() ? OUTCOME.SUCCESS : OUTCOME.FAILURE;
        if (outcome.equals(OUTCOME.SUCCESS)) {
            printWriter.println(format("%s|index|%d|document classified|outcome|%s|type|%s|millis|%d",
                                       now,
                                       index,
                                       outcome.toString(),
                                       documentTypeOfSample(),
                                       millis
            ));
            return;
        }

        printWriter.println(format("%s|index|%d|document classified|outcome|%s|error|%s|millis|%d",
                                   now,
                                   index,
                                   outcome.toString(),
                                   errorTypeOfSample(),
                                   millis
        ));
    }

    private static OutputStream openFileOutputStream(File file) {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        GenerateSampleDataConfig config = CliFactory.parseArguments(GenerateSampleDataConfig.class, args);

        System.out.println("generating sample data|size|" + config.getSamples() + "|start");
        try (PrintWriter printWriter = new PrintWriter(Optional.ofNullable(config.getFile()).map(GenerateSampleData::openFileOutputStream).orElse(System.out))) {
            IntStream.range(0, config.getSamples()).forEach(index -> printSample(index, printWriter));
            System.out.println("generating sample data|size|" + config.getSamples() + "|done");
        }

    }

}
