package org.phenoapps.verify;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;

public class AboutActivity extends MaterialAboutActivity {


    private CircularProgressDrawable progress;
    private MaterialAboutActionItem updateCheckItem;




    @NonNull
    @Override
    protected MaterialAboutList getMaterialAboutList(@NonNull Context context) {

        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();

        appCardBuilder.addItem(new MaterialAboutTitleItem.Builder().text("CheckList").icon(R.mipmap.ic_launcher).build());

        appCardBuilder.addItem(ConvenienceBuilder.createVersionActionItem(this,
                getResources().getDrawable(R.drawable.ic_about),
                "Version",
                false));

        MaterialAboutCard.Builder authorCardBuilder = new MaterialAboutCard.Builder();
        authorCardBuilder.title("Developers");

        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_chaney))
                .subText("\t\t"+getString(R.string.ksu))
                .icon(R.drawable.ic_person_profile)
                .build());
        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_trevor))
                .subText("\t\t"+getString(R.string.ksu)+"\n\t\t"+getString(R.string.dev_trevor_email))
                .icon(R.drawable.ic_person_profile)
                .build());
        authorCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(getString(R.string.dev_jesse))
                .subText("\t\t"+getString(R.string.ksu)+"\n\t\t"+getString(R.string.dev_jesse_email)+
                        "\n\t\t"+"http://wheatgenetics.org")
                .icon(R.drawable.ic_person_profile)
                .build());

        MaterialAboutCard.Builder descriptionCard = new MaterialAboutCard.Builder();
        descriptionCard.title("Description");
        descriptionCard.addItem(new MaterialAboutActionItem.Builder()
                .text("Verify is an Android application that imports a list of entries, scans barcodes, and " +
                        "identifies whether it exists in the list of entries along with audio/visual notifications.").build());

        return new MaterialAboutList(appCardBuilder.build(),authorCardBuilder.build(), descriptionCard.build());
    }

    @Nullable
    @Override
    protected CharSequence getActivityTitle() {
        return "About";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progress = new CircularProgressDrawable(this);
        progress.setStyle(CircularProgressDrawable.DEFAULT);
        progress.start();
    }
}