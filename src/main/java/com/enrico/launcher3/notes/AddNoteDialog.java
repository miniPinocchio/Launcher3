package com.enrico.launcher3.notes;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.enrico.launcher3.Launcher;
import com.enrico.launcher3.R;
import com.enrico.launcher3.theme.ThemeUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Enrico on 25/07/2017.
 */

public class AddNoteDialog extends DialogFragment {

    public static void show(Activity activity, boolean show) {

        String tag = "addMe";

        FragmentManager fm = activity.getFragmentManager();
        DialogFragment dialogFragment;

        if (show) {
            dialogFragment = new AddNoteDialog();
            dialogFragment.show(fm, tag);

        } else {

            dialogFragment = (DialogFragment) fm.findFragmentByTag(tag);

            if (dialogFragment != null) {
                dialogFragment.dismiss();
            }
        }

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final ViewGroup nullParent = null;
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        View dialogView = layoutInflater.inflate(R.layout.add_note_layout, nullParent, false);
        View titleView = layoutInflater.inflate(R.layout.dialog_custom_title, nullParent, false);
        TextView dialogTitle = titleView.findViewById(R.id.dialogTitle);

        final EditText title = dialogView.findViewById(R.id.title);
        final EditText note = dialogView.findViewById(R.id.note);
        RecyclerView priorities_rv = dialogView.findViewById(R.id.priorities_rv);
        TextView priority = dialogView.findViewById(R.id.priority);

        GridLayoutManager layoutManager
                = new GridLayoutManager(getActivity(), 5);

        priorities_rv.setHasFixedSize(true);
        priorities_rv.setLayoutManager(layoutManager);

        //create an array of colors that will populate the recycler view
        final Integer[] priorities_colors = new Integer[]{

                R.color.neutral_priority,
                R.color.material_green_400,
                R.color.material_yellow_400,
                R.color.material_orange_400,
                R.color.material_red_400,
        };

        //set the recycler view adapter and pass arguments to the adapter to it
        priorities_rv.setAdapter(new PrioritiesAdapter(getActivity(), priority, priorities_colors));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(dialogView);

        dialogTitle.setShadowLayer(1.5f, -1, 1, ThemeUtils.getColorAccent(getActivity()));
        dialogTitle.setText(R.string.add_note);
        builder.setCustomTitle(titleView);

        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                SQLiteDatabase notesDB = getActivity().openOrCreateDatabase("notesDB", MODE_PRIVATE, null);

                ArrayList<String> titles, notes, dates, priorities;

                titles = NotesUtils.initNoteElement(getActivity(), NoteField.TITLES);
                notes = NotesUtils.initNoteElement(getActivity(), NoteField.NOTES);
                dates = NotesUtils.initNoteElement(getActivity(), NoteField.DATES);
                priorities = NotesUtils.initNoteElement(getActivity(), NoteField.PRIORITIES);

                String priority = PrioritiesAdapter.priority();

                String addedTitle = title.getText().toString();

                String addedNote = note.getText().toString();

                DateFormat df = new SimpleDateFormat("EEE, d MMM yyyy, HH:mm:ss", java.util.Locale.getDefault());
                String date = df.format(Calendar.getInstance().getTime());

                titles.add(addedTitle + date);
                notes.add(addedNote + date);
                dates.add(date);
                priorities.add(priority + date);

                NotesRecyclerViewAdapter notesRecyclerViewAdapter = new NotesRecyclerViewAdapter(getActivity(), titles, notes, priorities, dates);

                RecyclerView notesRecyclerView = getActivity().findViewById(R.id.notesRecyclerView);

                notesRecyclerView.getAdapter().notifyDataSetChanged();

                notesRecyclerView.setAdapter(notesRecyclerViewAdapter);

                //fill the titles column
                notesDB.execSQL("insert into titles (title) values(?);", new String[]{title.getText().toString() + date});

                //fill the notes column
                notesDB.execSQL("insert into notes (note) values(?);", new String[]{note.getText().toString() + date});

                //fill the dates column
                notesDB.execSQL("insert into dates (date) values(?);", new String[]{date});

                //fill the priority column
                notesDB.execSQL("insert into priorities (priority) values(?);", new String[]{String.valueOf(priority + date)});

                notesDB.close();

                dismiss();
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // do something
        Launcher.closeKeyboard(getActivity());
    }
}