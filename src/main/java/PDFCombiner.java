import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PDFCombiner {
    private final List<PDDocument> openList = new ArrayList<>();
    private final List<File> sources = new ArrayList<>();
    private Path output;
    private Path input;

    public PDFCombiner() {

    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Missing args");
            System.exit(1);
        }

        final ArgumentParser parser = ArgumentParsers.newFor("PDFCombiner").build()
                .defaultHelp(true)
                .description("Combine multiple PDF files");
        parser.addArgument("-o", "--output")
                .required(false)
                .setDefault("")
                .nargs("?");
        parser.addArgument("-i", "--input")
                .required(true)
                .setDefault("./")
                .nargs("?");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        final PDFCombiner combiner = new PDFCombiner();
        System.out.println(ns.getString("input"));
        combiner.setInput(ns.getString("input"));
        combiner.setOutput(ns.getString("output"));
        try {
            combiner.combine();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public void setOutput(String path) {
        if (path.length() == 0) {
            output = Paths.get(input.toString(), "combined.pdf");
            return;
        }
        output = Paths.get(path);
    }

    public void setInput(String path) {
        input = Paths.get(path);
    }

    private void scanInput() {
        final File file = input.toFile();
        if (!file.exists()) return;
        if (file.isDirectory()) {
            final List<File> sources = searchDirectory(file);
            if (sources != null) this.sources.addAll(sources);
        } else if (isPDF(file)) {
            sources.add(file);
        }
    }

    private List<File> searchDirectory(File directory) {
        if (!directory.exists() || !directory.isDirectory()) return null;
        final List<File> files = new ArrayList<>();
        final File[] children = directory.listFiles();
        if (children == null) return null;
        for (final File child : children) {
            if (child.isDirectory()) {
                final List<File> childPdfList = searchDirectory(child);
                if (childPdfList == null) continue;
                files.addAll(childPdfList);
            } else if (isPDF(child)) {
                files.add(child);
            }
        }
        return files;
    }

    private boolean isPDF(File source) {
        return source.getName().toLowerCase().endsWith(".pdf");
    }


    private void closeOpened() {
        if (openList.isEmpty()) return;
        for (PDDocument document : openList) {
            try {
                document.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void combine() throws IOException {
        System.out.println(output.toString());
        final PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        pdfMergerUtility.setDestinationFileName(output.toString());
        System.out.println(pdfMergerUtility.getDestinationFileName());

        scanInput();
        for (final File file : sources) {
            openList.add(PDDocument.load(file));
            pdfMergerUtility.addSource(file);
        }
        pdfMergerUtility.mergeDocuments(null);
        System.out.println(pdfMergerUtility.getDestinationFileName());
        closeOpened();
    }
}
