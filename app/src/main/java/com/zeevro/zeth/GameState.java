package com.zeevro.zeth;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import java.util.Arrays;
import java.util.Vector;

public class GameState {
    static final int ANIMATE_MASK = 4096;
    private static final int BLANK_CARD = 4095;

    static final int[] DEFAULT_COLORS = {0xFF3A9C3A, 0xFFD33A3A, 0xFF7D3A7D, 0xFF0000CC};
    private static final int MAX_COLORS = DEFAULT_COLORS.length;

    static final int SCREEN_GAME = 0;
    static final int SCREEN_STATISTICS = 1;
    static final int SCREEN_PREFERENCES = 2;
    static final int SCREEN_HELP = 3;
    static final int SCREEN_MAIN = 4;

    int[] animate;
    boolean animations = true;
    boolean autoDeal = true;
    int bestTime;
    int[] cardsDealt;
    boolean cheatedDuringCurrentGame;
    int[] colors = DEFAULT_COLORS.clone();
    private int[] deck;
    int deckPos;
    private final String[] descColors;
    private final String[] descFills;
    private final String[] descShapes;
    int elapsed;
    private final GameEndedDelegate endDelegate;
    int errors;
    int errorsAllTime;
    boolean gameOver;
    int gamesPlayed;
    int hintCount = 0;
    int[] hintSet = null;
    private int[] lastCardCache;
    int ntraits;
    int numDealt;
    int numSelected;
    int nvariants;
    int screen = SCREEN_MAIN;
    boolean[] selected;
    int sets;
    int setsAllTime;
    boolean showHintButton;
    boolean showingSplashScreen;
    private long startTime;
    int totalTime;
    boolean vibrate_on_error = true;
    boolean vibrate_on_toggle = true;

    public interface GameEndedDelegate {
        void gameEnded();
    }

    int getNCards() {
        int ret = nvariants;
        for (int i = 1; i < ntraits; ++i) {
            ret *= nvariants;
        }
        return ret;
    }

    int getMinBoard() {
        if (this.ntraits == 4 && this.nvariants == 3) {
            return 12;
        }
        if (this.ntraits == 4 && this.nvariants == 4) {
            return 16;
        }
        if (this.ntraits == 3 && this.nvariants == 3) {
            return 12;
        }
        throw new RuntimeException("Min board unknown for ntraits=" + ntraits + " nvariants=" + nvariants);
    }

    int getMaxBoard() {
        if (this.ntraits == 4 && this.nvariants == 3) {
            return 21;
        }
        if (this.ntraits == 4 && this.nvariants == 4) {
            return 40;
        }
        if (this.ntraits == 3 && this.nvariants == 3) {
            return 15;
        }
        throw new RuntimeException("Max board unknown for ntraits=" + ntraits + " nvariants=" + nvariants);
    }

    int getNumRows() {
        return nvariants;
    }

    int getMaxColumns() {
        return getMaxBoard() / getNumRows();
    }

    GameState(GameEndedDelegate gameEndDelegate) {
        this.descShapes = new String[]{"oval", "diamond", "bean", "rectangle"};
        this.descFills = new String[]{"solid", "stripe1", "empty", "stripe2"};
        this.descColors = new String[]{"green", "red", "purple", "blue"};
        this.bestTime = 10000;
        this.gameOver = true;
        this.endDelegate = gameEndDelegate;
    }

    private int getOrThrowInt(SharedPreferences prefs, String name) throws Exception {
        int val = prefs.getInt(name, -1);
        if (val != -1) {
            return val;
        }
        throw new Exception("not found");
    }

    void pauseClock() {
        this.elapsed = (int) ((System.currentTimeMillis() - this.startTime) / 1000);
    }

    void restartClock() {
        this.startTime = System.currentTimeMillis() - ((long) (this.elapsed * 1000));
    }

    boolean Load(SharedPreferences prefs) {
        try {
            this.ntraits = prefs.getInt("ntraits", 4);
            this.nvariants = prefs.getInt("nvariants", 3);
            this.deck = new int[getNCards()];
            this.cardsDealt = new int[getMaxBoard()];
            this.animate = new int[getMaxBoard()];
            this.selected = new boolean[getMaxBoard()];
            for (int i = 0; i < getNCards(); ++i) {
                deck[i] = getOrThrowInt(prefs, "deck" + i);
            }
            for (int i = 0; i < getMaxBoard(); ++i) {
                cardsDealt[i] = getOrThrowInt(prefs, "cardsDealt" + i);
            }
            for (int i = 0; i < MAX_COLORS; ++i) {
                colors[i] = prefs.getInt("color" + i, DEFAULT_COLORS[i]);
            }
            this.showHintButton = prefs.getBoolean("show_hint_button", false);
            this.autoDeal = prefs.getBoolean("auto_deal", true);
            this.animations = prefs.getBoolean("animations", true);
            this.vibrate_on_toggle = prefs.getBoolean("vibrate_on_toggle", true);
            this.vibrate_on_error = prefs.getBoolean("vibrate_on_error", true);
            this.cheatedDuringCurrentGame = prefs.getBoolean("cheated", false);
            this.numDealt = getOrThrowInt(prefs, "numDealt");
            this.deckPos = getOrThrowInt(prefs, "deckPos");
            this.numSelected = getOrThrowInt(prefs, "numSelected");
            for (int i = 0; i < getMaxBoard(); ++i) {
                this.selected[i] = prefs.getBoolean("selected" + i, false);
            }
            this.elapsed = getOrThrowInt(prefs, "elapsed");
            this.sets = getOrThrowInt(prefs, "sets");
            this.errors = getOrThrowInt(prefs, "errors");
            this.screen = prefs.getInt("currentScreen", -1);
            Log.d("Zeth", "Screen read as " + screen);
            if (screen < 0) {
                this.screen = SCREEN_HELP;
            }
            this.gameOver = prefs.getBoolean("gameOver", screen == SCREEN_STATISTICS);
            this.gamesPlayed = getOrThrowInt(prefs, "gamesPlayed");
            this.setsAllTime = getOrThrowInt(prefs, "setsAllTime");
            this.errorsAllTime = getOrThrowInt(prefs, "errorsAllTime");
            this.totalTime = getOrThrowInt(prefs, "totalTime");
            this.bestTime = getOrThrowInt(prefs, "bestTime");
            initLastCardCache();
            if (screen != 0) {
                return true;
            }
            this.startTime = System.currentTimeMillis() - ((long) (this.elapsed * 1000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    void Save(SharedPreferences prefs) {
        if (numDealt != 0 || screen != 0) {
            if (screen == 0) {
                this.elapsed = (int) ((System.currentTimeMillis() - this.startTime) / 1000);
            }
            Editor edit = prefs.edit();
            edit.putInt("ntraits", ntraits);
            edit.putInt("nvariants", nvariants);
            for (int i = 0; i < getNCards(); ++i) {
                edit.putInt("deck" + i, deck[i]);
            }
            for (int i = 0; i < getMaxBoard(); ++i) {
                edit.putInt("cardsDealt" + i, cardsDealt[i]);
            }
            for (int i = 0; i < MAX_COLORS; ++i) {
                edit.putInt("color" + i, colors[i]);
            }
            edit.putBoolean("show_hint_button", this.showHintButton);
            edit.putBoolean("auto_deal", autoDeal);
            edit.putBoolean("animations", animations);
            edit.putBoolean("vibrate_on_toggle", vibrate_on_toggle);
            edit.putBoolean("vibrate_on_error", vibrate_on_error);
            edit.putBoolean("cheated", cheatedDuringCurrentGame);
            edit.putInt("numDealt", numDealt);
            edit.putInt("deckPos", deckPos);
            edit.putInt("numSelected", numSelected);
            for (int i = 0; i < getMaxBoard(); ++i) {
                edit.putBoolean("selected" + i, selected[i]);
            }
            edit.putInt("elapsed", elapsed);
            edit.putInt("sets", sets);
            edit.putInt("errors", errors);
            edit.putInt("currentScreen", screen);
            Log.d("Zeth", "Screen written as " + screen);
            edit.putBoolean("gameOver", gameOver);
            edit.putInt("gamesPlayed", gamesPlayed);
            edit.putInt("setsAllTime", setsAllTime);
            edit.putInt("errorsAllTime", errorsAllTime);
            edit.putInt("totalTime", totalTime);
            edit.putInt("bestTime", bestTime);
            edit.commit();
        }
    }

    void startGame(int ntraits2, int nvariants2) {
        if (gameOver) {
            this.gameOver = false;
            this.ntraits = ntraits2;
            this.nvariants = nvariants2;
            this.deck = new int[getNCards()];
            this.cardsDealt = new int[getMaxBoard()];
            this.animate = new int[getMaxBoard()];
            this.selected = new boolean[getMaxBoard()];
            initLastCardCache();
            for (int i = 0; i < getNCards(); ++i) {
                deck[i] = i;
            }
            shuffle(deck);
            this.numSelected = 0;
            this.numDealt = 0;
            this.deckPos = 0;
            for (int i = 0; i < selected.length; ++i) {
                selected[i] = false;
            }
            this.elapsed = 0;
            this.errors = 0;
            this.sets = 0;
            this.startTime = System.currentTimeMillis();
            Deal(getMinBoard());
            while (NoSets()) {
                Deal(nvariants2);
            }
            this.cheatedDuringCurrentGame = false;
        }
    }

    private static void shuffle(int[] arr) {
        for (int i = 0; i < arr.length - 1; ++i) {
            int n = (int) (Math.random() * ((double) (arr.length - i)));
            if (n != 0) {
                int t = arr[i];
                arr[i] = arr[i + n];
                arr[i + n] = t;
            }
        }
    }

    void Deal(int n) {
        for (int i = 0; i < n; ++i) {
            cardsDealt[numDealt++] = deck[deckPos++];
        }
    }

    private void initLastCardCache() {
        this.lastCardCache = new int[(int)Math.pow(nvariants, nvariants - 1)];
        Arrays.fill(lastCardCache, -1);
        for (int i = 0; i < nvariants; ++i) {
            int idx = 0;
            for (int j = 0; j < nvariants - 1; ++j) {
                idx = (nvariants * idx) + i;
            }
            this.lastCardCache[idx] = i;
        }
        for (int i = 0; i < nvariants; ++i) {
            for (int j = 0; j < nvariants; ++j) {
                if (i != j) {
                    if (nvariants == 3) {
                        lastCardCache[(i * 3) + j] = (3 - i) - j;
                    } else {
                        for (int k = 0; k < nvariants; ++k) {
                            if (!(k == i || k == j)) {
                                if (nvariants == 4) {
                                    lastCardCache[(i * 16) + (j * 4) + k] = ((6 - i) - j) - k;
                                } else {
                                    throw new RuntimeException("nvariants " + nvariants + " not written");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void ToggleCard(int n) {
        if (selected[n]) {
            selected[n] = false;
            numSelected--;
            return;
        }
        this.selected[n] = true;
        this.numSelected++;
    }

    int getDivisor() {
        return getNCards() / nvariants;
    }

    private Vector<int[]> GetSets() {
        int[] present = new int[getNCards()];
        Arrays.fill(present, -1);
        for (int i = 0; i < this.numDealt; ++i) {
            present[cardsDealt[i]] = i;
        }
        Vector<int[]> sets2 = new Vector<>();
        GetSets(present, 0, numDealt - (nvariants - 1), 0, new int[(nvariants - 1)], sets2);
        return sets2;
    }

    boolean NoSets() {
        return GetSets().isEmpty();
    }

    public void endStatisticsScreen() {
        screen = 0;
        if (!this.gameOver) {
            restartClock();
        }
    }

    void GetSets(int[] present, int lo, int hi, int nesting, int[] others, Vector<int[]> sets2) {
        if (nesting == nvariants - 1) {
            int seek = 0;
            int m = 1;
            int i = 0;
            while (i < ntraits) {
                int otherVal = 0;
                for (int other : others) {
                    otherVal = (nvariants * otherVal) + ((other / m) % nvariants);
                }
                int last = lastCardCache[otherVal];
                if (last != -1) {
                    seek += m * last;
                    ++i;
                    m *= nvariants;
                } else {
                    return;
                }
            }
            if (present[seek] != -1) {
                int[] newSet = new int[nvariants];
                for (i = 0; i < nvariants - 1; ++i) {
                    newSet[i] = present[others[i]];
                }
                newSet[nvariants - 1] = present[seek];
                sets2.add(newSet);
                showFoundSet(others, seek);
                return;
            }
            return;
        }
        for (int i = lo; i <= hi; ++i) {
            others[nesting] = cardsDealt[i];
            GetSets(present, i + 1, hi + 1, nesting + 1, others, sets2);
        }
    }

    int[] FindASet() {
        Vector<int[]> sets2 = GetSets();
        if (sets2.isEmpty()) {
            return null;
        }
        int[] ret = sets2.elementAt((int) (Math.random() * sets2.size()));
        shuffle(ret);
        return ret;
    }

    void CheckSet(ZethView zethView) {
        int nsel = 0;
        int[] selidx = new int[nvariants];
        int[] sel = new int[nvariants];
        for (int i = 0; i < numDealt; ++i) {
            if (selected[i]) {
                sel[nsel] = cardsDealt[i];
                int nsel2 = nsel + 1;
                selidx[nsel] = i;
                nsel = nsel2;
            }
        }
        for (int i = 0; i < selected.length; ++i) {
            selected[i] = false;
        }
        numSelected = 0;
        hintCount = 0;
        if (IsSet(sel)) {
            hintSet = null;
            for (int i = 0; i < getMaxBoard(); ++i) {
                animate[i] = 0;
            }
            this.sets++;
            if (numDealt == getMinBoard() && deckPos < getNCards()) {
                Deal(nvariants);
            }
            for (int i = 0; i < nvariants; ++i) {
                animate[selidx[i]] = cardsDealt[selidx[i]] + ANIMATE_MASK;
                cardsDealt[selidx[i]] = BLANK_CARD;
            }
            for (int i = 0; i < nvariants; ++i) {
                if (selidx[i] < numDealt - nvariants) {
                    int j = 0;
                    while (true) {
                        if (j >= nvariants) {
                            break;
                        } else if (cardsDealt[(numDealt - nvariants) + j] != BLANK_CARD) {
                            cardsDealt[selidx[i]] = cardsDealt[(numDealt - nvariants) + j];
                            cardsDealt[(numDealt - nvariants) + j] = BLANK_CARD;
                            break;
                        } else {
                            j++;
                        }
                    }
                }
            }
            this.numDealt -= nvariants;
            zethView.startFlip();
            ensureSetPresent();
            return;
        }
        zethView.doError();
    }

    void ensureSetPresent() {
        while (NoSets()) {
            if (deckPos == getNCards()) {
                EndGame();
                this.gameOver = true;
                endDelegate.gameEnded();
                return;
            } else if (autoDeal) {
                Deal(nvariants);
            } else {
                return;
            }
        }
    }

    private boolean same(int[] cards) {
        for (int i = 1; i < cards.length; ++i) {
            if (cards[0] % nvariants != cards[i] % nvariants) {
                return false;
            }
        }
        return true;
    }

    private boolean diff(int[] cards) {
        int[] count = new int[nvariants];

        for (int card : cards) {
            count[card % nvariants]++;
        }

        for (int value : count) {
            if (value != 1) {
                return false;
            }
        }

        return true;
    }

    private boolean IsSet(int[] cards) {
        for (int i = 0; i < ntraits; ++i) {
            if (!same(cards) && !diff(cards)) {
                return false;
            }
            for (int j = 0; j < cards.length; ++j) {
                cards[j] /= nvariants;
            }
        }
        return true;
    }

    void EndGame() {
        if (!cheatedDuringCurrentGame) {
            this.elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000);
            if (elapsed < bestTime) {
                this.bestTime = elapsed;
            }
            this.totalTime += elapsed;
            this.gamesPlayed++;
        }
        this.setsAllTime += sets;
        this.errorsAllTime += errors;
    }

    private String getCardDesc(int val) {
        StringBuilder sb = new StringBuilder();

        sb.append(descShapes[val % nvariants]);

        int i = val / nvariants;

        if (ntraits >= 4) {
            sb.append('-');
            sb.append(descFills[i % nvariants]);
            i /= nvariants;
        }

        sb.append('-');
        sb.append(descColors[i % nvariants]);
        sb.append('-');
        sb.append(((i / nvariants) % nvariants) + 1);

        return sb.toString();
    }

    private void showFoundSet(int[] others, int seek) {
        if (Math.random() >= 10.0d) {
            StringBuilder sb = new StringBuilder("Zeth found:");

            for (int other : others) {
                sb.append(" ");
                sb.append(getCardDesc(other));
            }

            sb.append(" ");
            sb.append(getCardDesc(seek));

            Log.d("Zeth", sb.toString());
        }
    }
}
