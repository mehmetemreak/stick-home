package io.github.mehmetemreak.stickhome;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NamePrompt {

    public interface Listener {
        void onNameSet(String name);
    }

    public static void show(Context context, Listener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 28);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#E6161A22"));
        bg.setCornerRadius(dp(context, 18));
        bg.setStroke(dp(context, 1), Color.parseColor("#2E7FE0F2"));
        card.setBackground(bg);

        TextView title = new TextView(context);
        title.setText("Sana nasıl hitap edelim?");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        card.addView(title);

        EditText input = new EditText(context);
        input.setHint("İsmini yaz...");
        input.setHintTextColor(Color.parseColor("#777777"));
        input.setTextColor(Color.WHITE);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setSingleLine(true);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                dp(context, 320), LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = dp(context, 16);
        input.setLayoutParams(inputParams);
        card.addView(input);

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(context, 14), 0, 0);

        TextView save = button(context, "Kaydet", "#8FEBFF");
        save.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                dialog.dismiss();
                listener.onNameSet(name);
            }
        });
        buttons.addView(save);

        // Empty name = "asked, declined": greeting shows without a name and we don't ask again.
        TextView skip = button(context, "Atla", "#999999");
        skip.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onNameSet("");
        });
        buttons.addView(skip);

        card.addView(buttons);

        dialog.setContentView(card);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.CENTER);
        }
        dialog.show();
        input.post(() -> {
            input.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_FORCED);
            }
        });
    }

    private static TextView button(Context context, String text, String color) {
        TextView b = new TextView(context);
        b.setText(text);
        b.setTextColor(Color.parseColor(color));
        b.setTextSize(15);
        int padH = dp(context, 16), padV = dp(context, 8);
        b.setPadding(padH, padV, padH, padV);
        b.setFocusable(true);
        b.setFocusableInTouchMode(true);
        b.setBackgroundResource(R.drawable.tile_focus_background);
        return b;
    }

    private static int dp(Context context, int value) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
