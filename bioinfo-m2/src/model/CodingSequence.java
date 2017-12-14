package model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles coding sequences
 *
 */
public class CodingSequence {

    public enum Nucleotide {
        A("A"),
        C("C"),
        G("G"),
        T("T");

        private final String letter;

        Nucleotide(String letter) {
            this.letter = letter;
        }

        public static boolean contains(String letter) {
            for (Nucleotide nucleotide: Nucleotide.values()) {
                if (nucleotide.name().equals(letter)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return letter;
        }
    }

    public enum InitCodon {
        ATA("ATA"),
        ATC("ATC"),
        ATG("ATG"),
        ATT("ATT"),
        CTG("CTG"),
        GTG("GTG"),
        TTA("TTA"),
        TTG("TTG");

        private final String codon;

        InitCodon(String codon) {
            this.codon = codon;
        }

        public static boolean contains(String codon) {
            for (InitCodon initCodon: InitCodon.values()) {
                if (initCodon.name().equals(codon)) {
                    return true;
                }
            }
            return false;
        }
    }

    public enum StopCodon {
        TAA("TAA"),
        TAG("TAG"),
        TGA("TGA");

        private final String codon;

        StopCodon(String codon) {
            this.codon = codon;
        }

        public static boolean contains(String codon) {
            for (StopCodon stopCodon: StopCodon.values()) {
                if (stopCodon.name().equals(codon)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static String[] nucleobase = { "A", "C", "G", "T" };
    public static String[] codon_init = { "ATA", "ATC", "ATG", "ATT", "CTG", "GTG", "TTA", "TTG" };
    public static String[] codon_stop = { "TAA", "TAG", "TGA" };

    /*Regex*/
    public static final String START_CDS_INFO = ">";
    public static final String REGEX_LOCATION = "\\[location=.*\\]";
    public static final String REGEX_LOCATOR = "\\d+..\\d+";
    public static final String REGEX_JOIN = "join\\("+REGEX_LOCATOR+"(,"+REGEX_LOCATOR+")*\\)";
    public static final String REGEX_COMPLEMENT = "complement\\(("+REGEX_LOCATOR+"|"+REGEX_JOIN+")\\)";
    public static final String REGEX_COMPLETE = "\\[location=("+REGEX_LOCATOR+"|"+REGEX_JOIN+"|"+REGEX_COMPLEMENT+")\\]";
    public static final String REGEX_ATGC = "[^ACGT]";

    public int startIndex;
    public int endIndex;
    public String sequence;

    public CodingSequence(int startIndex, int endIndex) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public String toString() {
        return "["+startIndex+","+endIndex+"]";
    }

    /**
     * Methode de test pour les regex
     * @param regex regex à chercher
     * @param test string à parser
     */
    public static void RegexTester(String regex, String test) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(test);
        if (m.find())
            System.out.println(m.group());
        else
            System.out.println("FAUX");
    }
}
