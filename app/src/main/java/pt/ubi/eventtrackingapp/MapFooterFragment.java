package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.firestore.GeoPoint;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapFooterFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFooterFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFooterFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "geoPoint";
    private static final String ARG_PARAM2 = "isAnUserClick";
    private static final String ARG_PARAM3 = "hasImages";

    // TODO: Rename and change types of parameters
    private CustomGeoPoint geoPoint;
    private boolean isAnUserClick;
    private boolean hasImages;

    private Button addImage_btn,viewImages_btn, deleteTrack_btn;

    private OnFragmentInteractionListener mListener;

    private Session session;

    public MapFooterFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFooterFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFooterFragment newInstance(String param1, boolean param2, boolean param3) {
        MapFooterFragment fragment = new MapFooterFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putBoolean(ARG_PARAM2, param2);
        args.putBoolean(ARG_PARAM3, param3);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
                geoPoint = getArguments().getParcelable(ARG_PARAM1);
                isAnUserClick = getArguments().getBoolean(ARG_PARAM2);
                hasImages = getArguments().getBoolean(ARG_PARAM3);
            }
        session = new Session(getActivity());
    }

    public boolean isOnThisPosition() {
        Location currentPosition =session.getCurrentLocation();
        return geoPoint.getLatitude() == currentPosition.getLatitude()  && geoPoint.getLongitude() == currentPosition.getLongitude();
    }

   private void hideAddImageButton() {
       addImage_btn.setVisibility(View.INVISIBLE);
    }

    private void hideResetTrack() {
        deleteTrack_btn.setVisibility(View.INVISIBLE);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map_footer, container, false);

        addImage_btn = view.findViewById(R.id.addImage_btn);
        viewImages_btn = view.findViewById(R.id.viewImages_btn);
        deleteTrack_btn = view.findViewById(R.id.deleteTrack_btn);
        if(!isAnUserClick || !isOnThisPosition() || session.getEvent().isClosed()) {
            hideAddImageButton();
            hideResetTrack();
        }

        if(!hasImages)
            viewImages_btn.setEnabled(false);

        viewImages_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ButtonCallback ) getActivity()).launchAction(1, geoPoint);
            }
        });
        addImage_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ButtonCallback ) getActivity()).launchAction(2, geoPoint);
            }
        });
        deleteTrack_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ButtonCallback ) getActivity()).launchAction(3, null);
            }
        });
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public interface ButtonCallback {

        //You can add parameters if you need it
        // 1 -- view Images
        // 2 -- new Image
        // 3 -- deleteTrack
        void launchAction(int action, CustomGeoPoint geoPoint);
    }

}
