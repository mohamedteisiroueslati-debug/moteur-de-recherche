package com.recherche.backend.indexation;

/**
 * Algorithme de Porter (M.F. Porter, "An algorithm for suffix stripping", 1980).
 *
 * Notations de l'article :
 *  - c = consonne, v = voyelle ("y" est une consonne en début de mot ou après une voyelle)
 *  - tout mot s'écrit [C](VC)^m[V] ; m est la "mesure" du radical
 *  - une règle (condition) S1 -> S2 remplace le suffixe S1 par S2 si le radical
 *    restant vérifie la condition (souvent m > 0)
 *
 * Exemples : retrieval -> retriev, information -> inform, indexing -> index,
 *            systems -> system, generalizations -> gener
 */
public class PorterStemmer implements Stemmer {

    @Override
    public String raciner(String mot) {
        if (mot == null || mot.length() <= 2) return mot;   // les mots très courts ne sont pas modifiés
        Mot m = new Mot(mot);
        m.etape1a();
        m.etape1b();
        m.etape1c();
        m.etape2();
        m.etape3();
        m.etape4();
        m.etape5();
        return m.resultat();
    }

    /** Le mot en cours de traitement. */
    private static final class Mot {

        private final char[] b;   // lettres du mot
        private int k;            // indice de la dernière lettre du mot courant
        private int j;            // indice de la dernière lettre du radical (avant le suffixe testé)

        Mot(String mot) {
            b = mot.toCharArray();
            k = b.length - 1;
        }

        String resultat() {
            return new String(b, 0, k + 1);
        }

        // ---------- outils ----------

        /** b[i] est-il une consonne ? */
        private boolean consonne(int i) {
            switch (b[i]) {
                case 'a': case 'e': case 'i': case 'o': case 'u':
                    return false;
                case 'y':
                    return i == 0 || !consonne(i - 1);
                default:
                    return true;
            }
        }

        /** Mesure m du radical b[0..j] : nombre de séquences VC. */
        private int mesure() {
            int n = 0;
            int i = 0;
            while (true) {                          // saute le [C] initial
                if (i > j) return n;
                if (!consonne(i)) break;
                i++;
            }
            i++;
            while (true) {
                while (true) {                      // saute les voyelles
                    if (i > j) return n;
                    if (consonne(i)) break;
                    i++;
                }
                i++;
                n++;                                // une séquence VC de plus
                while (true) {                      // saute les consonnes
                    if (i > j) return n;
                    if (!consonne(i)) break;
                    i++;
                }
                i++;
            }
        }

        /** Le radical b[0..j] contient-il une voyelle ? (*v*) */
        private boolean voyelleDansRadical() {
            for (int i = 0; i <= j; i++) {
                if (!consonne(i)) return true;
            }
            return false;
        }

        /** b[i-1..i] est-il une double consonne ? (*d) */
        private boolean doubleConsonne(int i) {
            return i >= 1 && b[i] == b[i - 1] && consonne(i);
        }

        /** b[i-2..i] est-il de la forme consonne-voyelle-consonne, la dernière n'étant pas w, x ou y ? (*o) */
        private boolean cvc(int i) {
            if (i < 2 || !consonne(i) || consonne(i - 1) || !consonne(i - 2)) return false;
            char c = b[i];
            return c != 'w' && c != 'x' && c != 'y';
        }

        /** Le mot finit-il par s ? Si oui, j pointe juste avant le suffixe. */
        private boolean finitPar(String s) {
            int l = s.length();
            int debut = k - l + 1;
            if (debut < 0) return false;
            for (int i = 0; i < l; i++) {
                if (b[debut + i] != s.charAt(i)) return false;
            }
            j = k - l;
            return true;
        }

        /** Remplace le suffixe b[j+1..k] par s (s n'est jamais plus long que le suffixe retiré). */
        private void remplacer(String s) {
            for (int i = 0; i < s.length(); i++) {
                b[j + 1 + i] = s.charAt(i);
            }
            k = j + s.length();
        }

        /** Remplace le suffixe par s seulement si m > 0. */
        private void remplacerSiM0(String s) {
            if (mesure() > 0) remplacer(s);
        }

        /**
         * Teste les règles (suffixe -> remplacement) dans l'ordre ; la première dont le
         * suffixe correspond est appliquée si m > 0, et on s'arrête là.
         */
        private void appliquerPremiere(String... regles) {
            for (int i = 0; i < regles.length; i += 2) {
                if (finitPar(regles[i])) {
                    remplacerSiM0(regles[i + 1]);
                    return;
                }
            }
        }

        // ---------- étapes de l'algorithme ----------

        /** 1a : pluriels. caresses -> caress, ponies -> poni, cats -> cat */
        void etape1a() {
            if (b[k] != 's') return;
            if (finitPar("sses")) k -= 2;
            else if (finitPar("ies")) remplacer("i");
            else if (b[k - 1] != 's') k--;
        }

        /** 1b : -eed, -ed, -ing. agreed -> agree, plastered -> plaster, motoring -> motor */
        void etape1b() {
            if (finitPar("eed")) {
                if (mesure() > 0) k--;
            } else if ((finitPar("ed") || finitPar("ing")) && voyelleDansRadical()) {
                k = j;
                if (finitPar("at")) remplacer("ate");           // conflat(ed) -> conflate
                else if (finitPar("bl")) remplacer("ble");      // troubl(ed) -> trouble
                else if (finitPar("iz")) remplacer("ize");      // siz(ed) -> size
                else if (doubleConsonne(k)) {                   // hopp(ing) -> hop
                    char c = b[k];
                    if (c != 'l' && c != 's' && c != 'z') k--;  // mais fall(ing) -> fall
                } else if (mesure() == 1 && cvc(k)) {           // fil(ing) -> file
                    remplacer("e");
                }
            }
        }

        /** 1c : y -> i s'il y a une voyelle avant. happy -> happi, sky -> sky */
        void etape1c() {
            if (finitPar("y") && voyelleDansRadical()) b[k] = 'i';
        }

        /** 2 : suffixes doubles. relational -> relate, digitizer -> digitize, ... (si m > 0) */
        void etape2() {
            if (k < 1) return;
            switch (b[k - 1]) {
                case 'a': appliquerPremiere("ational", "ate", "tional", "tion"); break;
                case 'c': appliquerPremiere("enci", "ence", "anci", "ance"); break;
                case 'e': appliquerPremiere("izer", "ize"); break;
                case 'l': appliquerPremiere("bli", "ble", "alli", "al", "entli", "ent", "eli", "e", "ousli", "ous"); break;
                case 'o': appliquerPremiere("ization", "ize", "ation", "ate", "ator", "ate"); break;
                case 's': appliquerPremiere("alism", "al", "iveness", "ive", "fulness", "ful", "ousness", "ous"); break;
                case 't': appliquerPremiere("aliti", "al", "iviti", "ive", "biliti", "ble"); break;
                case 'g': appliquerPremiere("logi", "log"); break;
                default: break;
            }
        }

        /** 3 : -ic-, -full, -ness, ... triplicate -> triplic, hopeful -> hope (si m > 0) */
        void etape3() {
            switch (b[k]) {
                case 'e': appliquerPremiere("icate", "ic", "ative", "", "alize", "al"); break;
                case 'i': appliquerPremiere("iciti", "ic"); break;
                case 'l': appliquerPremiere("ical", "ic", "ful", ""); break;
                case 's': appliquerPremiere("ness", ""); break;
                default: break;
            }
        }

        /** 4 : retire -ant, -ence, -ment, ... si m > 1. adjustment -> adjust, adoption -> adopt */
        void etape4() {
            if (k < 1) return;
            switch (b[k - 1]) {
                case 'a': if (finitPar("al")) break; return;
                case 'c': if (finitPar("ance") || finitPar("ence")) break; return;
                case 'e': if (finitPar("er")) break; return;
                case 'i': if (finitPar("ic")) break; return;
                case 'l': if (finitPar("able") || finitPar("ible")) break; return;
                case 'n': if (finitPar("ant") || finitPar("ement") || finitPar("ment") || finitPar("ent")) break; return;
                case 'o':
                    // -ion seulement après s ou t : adoption -> adopt, mais pas onion
                    if (finitPar("ion") && j >= 0 && (b[j] == 's' || b[j] == 't')) break;
                    if (finitPar("ou")) break;
                    return;
                case 's': if (finitPar("ism")) break; return;
                case 't': if (finitPar("ate") || finitPar("iti")) break; return;
                case 'u': if (finitPar("ous")) break; return;
                case 'v': if (finitPar("ive")) break; return;
                case 'z': if (finitPar("ize")) break; return;
                default: return;
            }
            if (mesure() > 1) k = j;
        }

        /** 5 : retire le -e final et réduit -ll. probate -> probat, controll -> control */
        void etape5() {
            j = k;
            if (b[k] == 'e') {
                int m = mesure();
                if (m > 1 || (m == 1 && !cvc(k - 1))) k--;
            }
            if (b[k] == 'l' && doubleConsonne(k) && mesure() > 1) k--;
        }
    }
}
