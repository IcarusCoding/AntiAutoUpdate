package de.intelligence.antiautoupdate.layout;

import org.jetbrains.annotations.NotNull;

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public final class AppEntrySpacing extends RecyclerView.ItemDecoration {

    private final int spacing;

    public AppEntrySpacing(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NonNull @NotNull Rect outRect, @NonNull @NotNull View view, @NonNull @NotNull RecyclerView parent, @NonNull @NotNull RecyclerView.State state) {
        outRect.bottom = spacing;
    }

}
