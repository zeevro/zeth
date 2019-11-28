package com.zeevro.zeth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zeevro.zeth.GameState.GameEndedDelegate;

import androidx.annotation.NonNull;
import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;

import static android.view.Window.FEATURE_PROGRESS;

public class Zeth extends Activity implements GameEndedDelegate {
    protected static final int MENU_END_GAME = 0;
    protected static final int MENU_STATISTICS = 1;
    protected static final int MENU_PREFERENCES = 2;
    protected static final int MENU_HELP = 3;

    private GameState game;
    private ZethView zethView;
    private WakeLock wakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(FEATURE_PROGRESS);
        this.wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getString(R.string.app_name));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadState();
        wakeLock.acquire();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveState();
        wakeLock.release();
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        game.restartClock();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_END_GAME, 0, getString(R.string.menu_end_game));
        menu.add(0, MENU_STATISTICS, 0, getString(R.string.menu_statistics));
        menu.add(0, MENU_PREFERENCES, 0, getString(R.string.menu_preferences));
        menu.add(0, MENU_HELP, 0, getString(R.string.menu_help));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case MENU_END_GAME:
                game.gameOver = true;
                prepareStatistics();
                return true;
            case MENU_STATISTICS:
                game.pauseClock();
                prepareStatistics();
                return true;
            case MENU_PREFERENCES:
                game.pauseClock();
                preparePreferences();
                return true;
            case MENU_HELP:
                game.pauseClock();
                prepareHelp();
                return true;
            default:
                return false;
        }
    }

    public void loadState() {
        this.game = new GameState(this);
        game.Load(getPreferences(MODE_PRIVATE));
        game.showingSplashScreen = false;

        this.zethView = new ZethView(this, game);

        switch (game.screen) {
            case GameState.SCREEN_GAME:
                prepareGameView(); break;
            case GameState.SCREEN_STATISTICS:
                prepareStatistics(); break;
            case GameState.SCREEN_PREFERENCES:
                preparePreferences(); break;
            case GameState.SCREEN_HELP:
                prepareHelp(); break;
            case GameState.SCREEN_MAIN:
                prepareMainView(); break;
        }
    }

    public void saveState() {
        game.Save(getPreferences(MODE_PRIVATE));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 4) {
            return super.onKeyDown(keyCode, event);
        }
        switch (game.screen) {
            case GameState.SCREEN_STATISTICS:
            case GameState.SCREEN_HELP:
                endStatisticsScreen();
                return true;
            case GameState.SCREEN_PREFERENCES:
                applyPreferences();
                endStatisticsScreen();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    public void endStatisticsScreen() {
        if (game.gameOver) {
            prepareMainView();
        } else {
            prepareGameView();
        }
    }

    public void prepareMainView() {
        game.screen = GameState.SCREEN_MAIN;
        setContentView(R.layout.main);
        findViewById(R.id.game_3_3).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                game.startGame(3, 3);
                prepareGameView();
            }
        });
        findViewById(R.id.game_3_4).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                game.startGame(4, 3);
                prepareGameView();
            }
        });
        findViewById(R.id.game_4_4).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                game.startGame(4, 4);
                prepareGameView();
            }
        });
        findViewById(R.id.game_stats).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                prepareStatistics();
            }
        });
    }

    public void prepareGameView() {
        setContentView(R.layout.game);

        game.screen = GameState.SCREEN_GAME;

        zethView.loadImages(this);

        if (game.cheatedDuringCurrentGame) {
            findViewById(R.id.game_layout).setBackgroundColor(ZethView.CHEATER_COLOR);
        }

        if (zethView.getParent() != null) {
            ((ViewGroup) zethView.getParent()).removeAllViews();
        }
        ((LinearLayout) findViewById(R.id.inner_layout)).addView(zethView, 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        findViewById(R.id.buttons).setVisibility((game.showHintButton || !game.autoDeal) ? View.VISIBLE : View.GONE);

        Button b = findViewById(R.id.hint_button);
        b.setVisibility(game.showHintButton ? View.VISIBLE : View.GONE);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (game.hintCount != game.nvariants - 1) {
                    if (game.hintSet == null) {
                        game.hintSet = game.FindASet();
                        if (game.hintSet == null) {
                            return;
                        }
                    }
                    setCheated();
                    game.numSelected = 0;
                    for (int i = 0; i < game.selected.length; ++i) {
                        game.selected[i] = false;
                    }
                    game.hintCount ++;
                    for (int i = 0; i < game.hintCount; ++i) {
                        game.numSelected ++;
                        game.selected[game.hintSet[i]] = true;
                    }
                    zethView.invalidate();
                }
            }
        });

        Button b2 = findViewById(R.id.plus3_button);
        b2.setVisibility(game.autoDeal ? View.GONE : View.VISIBLE);
        b2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!game.NoSets()) {
                    zethView.doError();
                } else if (game.deckPos < 81) {
                    game.Deal(Zeth.MENU_HELP);
                    zethView.invalidate();
                } else {
                    game.EndGame();
                    game.gameOver = true;
                    prepareStatistics();
                }
            }
        });

        game.ensureSetPresent();
    }

    public void gameEnded() {
        prepareStatistics();
    }

    @SuppressLint("SetTextI18n")
    public void prepareStatistics() {
        game.screen = GameState.SCREEN_STATISTICS;

        setContentView(R.layout.statistics);

        if (game.cheatedDuringCurrentGame) {
            findViewById(R.id.statistics_layout).setBackgroundColor(ZethView.CHEATER_COLOR);
            findViewById(R.id.cheater_label).setVisibility(View.VISIBLE);
        }

        String title = getString(game.gameOver ? R.string.end_game_over : R.string.end_statistics);
        String cheatStar = game.cheatedDuringCurrentGame ? " *" : "";
        int myTotalTime = game.totalTime + game.elapsed;

        setTitle(getString(R.string.app_name) + ": " + title);

        setProgress(10000);

        Button b = findViewById(R.id.back_to_game);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                endStatisticsScreen();
            }
        });
        b.setText(getString(game.gameOver ? R.string.new_game : R.string.back_to_game));

        ((TextView) findViewById(R.id.end_title)).setText(title);
        ((TextView) findViewById(R.id.end_value_elapsed)).setText(String.format(getString(R.string.end_value_elapsed) + cheatStar, game.elapsed, game.elapsed == 1 ? R.string.second : R.string.seconds));
        ((TextView) findViewById(R.id.end_value_fastest)).setText(game.bestTime != 10000 ? String.format(getString(R.string.end_value_fastest) + cheatStar, game.bestTime) : "");
        ((TextView) findViewById(R.id.end_value_average)).setText(game.gamesPlayed > 0 ? String.format(getString(R.string.end_value_average) + cheatStar, (float)game.totalTime / (float)game.gamesPlayed) : "");
        ((TextView) findViewById(R.id.end_value_sets_found)).setText(Integer.toString(game.sets));
        ((TextView) findViewById(R.id.end_value_errors)).setText(Integer.toString(game.errors));
        ((TextView) findViewById(R.id.end_value_games_played)).setText(Integer.toString(game.gamesPlayed));
        ((TextView) findViewById(R.id.end_value_sets_all_time)).setText(Integer.toString(game.setsAllTime));
        ((TextView) findViewById(R.id.end_value_errors_all_time)).setText(Integer.toString(game.errorsAllTime));
        ((TextView) findViewById(R.id.end_value_playing_time)).setText(String.format(getString(R.string.end_value_playing_time), myTotalTime / 3600, (myTotalTime / 60) % 60, myTotalTime % 60));
    }

    private void reshowColors() {
        ((Button) findViewById(R.id.color1)).setTextColor(game.colors[0]);
        ((Button) findViewById(R.id.color2)).setTextColor(game.colors[1]);
        ((Button) findViewById(R.id.color3)).setTextColor(game.colors[2]);
        ((Button) findViewById(R.id.color4)).setTextColor(game.colors[3]);
    }

    private void showColorDialog(final int idx) {
        new AmbilWarnaDialog(this, game.colors[idx], new OnAmbilWarnaListener() {
            public void onOk(AmbilWarnaDialog dialog, int color) {
                game.colors[idx] = color;
                reshowColors();
            }

            public void onCancel(AmbilWarnaDialog dialog) {
            }
        }).show();
    }

    private void applyPreferences() {
        zethView.loadImages(this);

        game.showHintButton = ((CheckBox) findViewById(R.id.show_hint_button)).isChecked();
        game.autoDeal = ((CheckBox) findViewById(R.id.auto_deal_button)).isChecked();
        game.animations = ((CheckBox) findViewById(R.id.animate_button)).isChecked();
        game.vibrate_on_toggle = ((CheckBox) findViewById(R.id.vibrate_toggle_button)).isChecked();
        game.vibrate_on_error = ((CheckBox) findViewById(R.id.vibrate_error_button)).isChecked();
    }

    public void preparePreferences() {
        game.screen = GameState.SCREEN_PREFERENCES;

        setContentView(R.layout.preferences);

        Button b = findViewById(R.id.back_to_game);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                applyPreferences();
                endStatisticsScreen();
            }
        });
        b.setText(R.string.back_to_game);

        findViewById(R.id.default_colors).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                game.colors = (int[]) GameState.DEFAULT_COLORS.clone();
                reshowColors();
            }
        });

        findViewById(R.id.color1).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showColorDialog(0);
            }
        });

        findViewById(R.id.color2).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showColorDialog(Zeth.MENU_STATISTICS);
            }
        });

        findViewById(R.id.color3).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showColorDialog(Zeth.MENU_PREFERENCES);
            }
        });

        findViewById(R.id.color4).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showColorDialog(Zeth.MENU_HELP);
            }
        });

        reshowColors();

        ((CheckBox) findViewById(R.id.show_hint_button)).setChecked(game.showHintButton);
        ((CheckBox) findViewById(R.id.auto_deal_button)).setChecked(game.autoDeal);
        ((CheckBox) findViewById(R.id.animate_button)).setChecked(game.animations);
        ((CheckBox) findViewById(R.id.vibrate_toggle_button)).setChecked(game.vibrate_on_toggle);
        ((CheckBox) findViewById(R.id.vibrate_error_button)).setChecked(game.vibrate_on_error);
    }

    public void prepareHelp() {
        game.screen = GameState.SCREEN_HELP;

        setContentView(R.layout.help);

        TextView tv = findViewById(R.id.help_text_view);
        tv.setLinksClickable(true);
        tv.setMovementMethod(new LinkMovementMethod());
        tv.setText(Html.fromHtml(getString(R.string.help_text)));
        tv.setTextColor(0xFFFFFFFF);
        tv.setLinkTextColor(0xFFCCDDFF);

        Button b = findViewById(R.id.back_to_game);
        b.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                endStatisticsScreen();
            }
        });
        b.setText(R.string.back_to_game);
    }

    public void setCheated() {
        if (!game.cheatedDuringCurrentGame) {
            game.cheatedDuringCurrentGame = true;
            findViewById(R.id.game_layout).setBackgroundColor(ZethView.CHEATER_COLOR);
            zethView.invalidate();
        }
    }
}
