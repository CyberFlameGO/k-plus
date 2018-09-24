package sheasmith.me.betterkamar.pages.details;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import sheasmith.me.betterkamar.ApiManager;
import sheasmith.me.betterkamar.R;
import sheasmith.me.betterkamar.dataModels.DetailsObject;
import sheasmith.me.betterkamar.dataModels.LoginObject;
import sheasmith.me.betterkamar.internalModels.ApiResponse;
import sheasmith.me.betterkamar.internalModels.Exceptions;
import sheasmith.me.betterkamar.internalModels.PortalObject;

import static android.view.View.GONE;

public class StudentDetailsFragment extends Fragment {

    private View mView;
    private ProgressBar mLoader;
    private PortalObject mPortal;

    public static StudentDetailsFragment newInstance() {
        return new StudentDetailsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PortalObject portal = (PortalObject) getActivity().getIntent().getSerializableExtra("portal");
        mPortal = portal;
        ApiManager.setVariables(portal, getContext());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        new Thread(new Runnable() {
            @Override
            public void run() {
                doRequest(mPortal);
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_details_student, container, false);
        mLoader = mView.findViewById(R.id.loader);

        return mView;
    }

    private void doRequest(final PortalObject portal) {
       ApiManager.getDetails(new ApiResponse<DetailsObject>() {
           @Override
           public void success(final DetailsObject value) {
               if (getActivity() == null) {
                   // TODO: Error?
                   return;
               }
               getActivity().runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       DetailsObject.Student student = value.StudentDetailsResults.Student;
                       ((TextView) mView.findViewById(R.id.name)).setText(String.format("%s %s", student.FirstName, student.LastName));
                       String studentPathName = getContext().getFilesDir().toString() + "/" + portal.studentFile;
                       ((CircleImageView) mView.findViewById(R.id.studentImage)).setImageDrawable(Drawable.createFromPath(studentPathName));
                       ((TextView) mView.findViewById(R.id.nsn)).setText(String.format("NSN: %s", student.NSN));

                       ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                       ClipData clip = ClipData.newPlainText("NSN", student.NSN);
                       clipboard.setPrimaryClip(clip);

                       setText(student.FirstNameLegal, R.id.legal_name_first_name, R.id.legal_name_first_name_heading);
                       setText(student.LastNameLegal, R.id.legal_name_last_name, R.id.legal_name_last_name_heading);
                       setText(student.ForeNamesLegal, R.id.legal_name_fore_names, R.id.legal_name_fore_names_heading);
                       maybeHideView(R.id.legal_name, student.FirstNameLegal, student.LastNameLegal, student.ForeNamesLegal);

                       setText(student.FirstName, R.id.preferred_name_first_name, R.id.preferred_name_first_name_heading);
                       setText(student.LastName, R.id.preferred_name_last_name, R.id.preferred_name_last_name_heading);
                       setText(student.ForeNames, R.id.preferred_name_fore_names, R.id.preferred_name_fore_names_heading);
                       maybeHideView(R.id.preferred_name, student.FirstName, student.LastName, student.ForeNames);

                       setText(student.DateBirth, R.id.other_information_date_of_birth, R.id.other_information_date_of_birth_heading);
                       setText(student.StudentEmail, R.id.other_information_email, R.id.other_information_email_heading);
                       setText(student.StudentSchoolEmail, R.id.other_information_email_school, R.id.other_information_email_school_heading);
                       setText(student.Ethnicity, R.id.other_information_ethnicity, R.id.other_information_ethnicity_heading);
                       setText(student.Gender, R.id.other_information_gender, R.id.other_information_gender_heading);
                       setText(student.Age, R.id.other_information_age, R.id.other_information_age_heading);
                       maybeHideView(R.id.other_information, student.DateBirth, student.StudentEmail, student.StudentSchoolEmail, student.Ethnicity, student.Age, student.Gender);

                       setText(student.ParentTitleB, R.id.residence_b_parent_title, R.id.residence_b_parent_title_heading);
                       setText(student.HomePhoneB, R.id.residence_b_home_phone, R.id.residence_b_home_phone_heading);
                       setText(student.HomeAddressB, R.id.residence_b_physical_address, R.id.residence_b_physical_address_heading);
                       setText(student.ParentEmailB, R.id.residence_b_parent_email, R.id.residence_b_parent_email_heading);
                       maybeHideView(R.id.residence_b, student.ParentTitleB, student.HomePhoneB, student.HomeAddressB, student.ParentEmailB);

                       setText(student.ParentTitle, R.id.residence_a_parent_title, R.id.residence_a_parent_title_heading);
                       setText(student.HomePhone, R.id.residence_a_home_phone, R.id.residence_a_home_phone_heading);
                       setText(student.HomeAddress, R.id.residence_a_physical_address, R.id.residence_a_physical_address_heading);
                       setText(student.ParentEmail, R.id.residence_a_parent_email, R.id.residence_a_parent_email);
                       maybeHideView(R.id.residence_a, student.ParentTitle, student.HomePhone, student.HomeAddress, student.ParentEmail);

                       mLoader.setVisibility(GONE);
                   }
               });
           }

           @Override
           public void error(Exception e) {
               e.printStackTrace();
               if (e instanceof Exceptions.ExpiredToken) {
                   ApiManager.login(portal.username, portal.password, new ApiResponse<LoginObject>() {
                       @Override
                       public void success(LoginObject value) {
                           doRequest(portal);
                       }

                       @Override
                       public void error(Exception e) {
                           e.printStackTrace();
                       }
                   });
                   return;
               } else if (e instanceof IOException) {
                   getActivity().runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           new AlertDialog.Builder(getContext())
                                   .setTitle("No Internet")
                                   .setMessage("You do not appear to be connected to the internet. Please check your connection and try again.")
                                   .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           doRequest(portal);
                                       }
                                   })
                                   .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialogInterface, int i) {
                                           getActivity().finish();
                                       }
                                   })
                                   .create()
                                   .show();
                       }
                   });
               }
           }
       });
    }

    private void setText(String text, int id, int header) {
        TextView view = mView.findViewById(id);
        if (text.equals("")) {
            view.setVisibility(GONE);
            mView.findViewById(header).setVisibility(GONE);
        }
        else
            view.setText(text);
    }

    private void maybeHideView(int id, String... items) {
        for (String s : items) {
            if (!s.equals("") && !s.equals(" "))
                return;
        }
        mView.findViewById(id).setVisibility(GONE);
    }
}