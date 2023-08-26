package de.intelligence.antiautoupdate.fragment;

import com.google.android.material.button.MaterialButton;

import org.jetbrains.annotations.NotNull;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.intelligence.antiautoupdate.R;

public final class RootDialogFragment extends DialogFragment {

    public interface RootDialogListener {

        void onDialogGrantClicked(RootDialogFragment rootDialogFragment);

    }

    private RootDialogListener listener;

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        try {
            this.listener = (RootDialogListener) context;
        } catch (ClassCastException ex) {
            throw new ClassCastException(context + " must implement " + RootDialogListener.class.getCanonicalName());
        }
    }

    @NonNull
    @NotNull
    @Override
    public Dialog onCreateDialog(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(super.getActivity());
        final LayoutInflater inflater = super.requireActivity().getLayoutInflater();
        final View rootDialogView = inflater.inflate(R.layout.root_dialog_fragment, null);
        final MaterialButton rootButton = rootDialogView.findViewById(R.id.rootButton);
        rootButton.setOnClickListener(v -> this.listener.onDialogGrantClicked(this));
        builder.setCancelable(false);
        builder.setView(rootDialogView);
        final Dialog dialog =  builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

}
